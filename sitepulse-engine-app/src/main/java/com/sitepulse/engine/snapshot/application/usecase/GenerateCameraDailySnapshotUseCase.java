package com.sitepulse.engine.snapshot.application.usecase;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.ExternalServiceException;
import com.sitepulse.engine.common.exception.ProcessingException;
import com.sitepulse.engine.common.exception.ResourceNotFoundException;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageEntity;
import com.sitepulse.engine.project.domain.model.Camera;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotAsset;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotSourceDecision;
import com.sitepulse.engine.snapshot.application.service.CameraSnapshotProfileService;
import com.sitepulse.engine.snapshot.application.service.SnapshotKeyFactory;
import com.sitepulse.engine.snapshot.application.service.WebImageTransformer;
import com.sitepulse.engine.snapshot.infrastructure.persistence.CameraDailySnapshotEntity;
import com.sitepulse.engine.snapshot.infrastructure.persistence.CameraDailySnapshotRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenerateCameraDailySnapshotUseCase {

    private final ResolveCameraSnapshotSourceUseCase resolveSourceUseCase;
    private final CameraSnapshotProfileService profileService;
    private final CameraDailySnapshotRepository snapshotRepository;
    private final SnapshotKeyFactory snapshotKeyFactory;
    private final WebImageTransformer webImageTransformer;
    private final ObjectStorage objectStorage;
    private final Clock clock;

    @Transactional
    public CameraSnapshotAsset generate(Project project, Camera camera, LocalDate snapshotDate, boolean force) {
        return doGenerate(project, camera, snapshotDate, force, null, null);
    }

    @Transactional
    public CameraSnapshotAsset generate(
            Project project,
            Camera camera,
            LocalDate snapshotDate,
            boolean force,
            DetectionImage importedImage,
            byte[] importedSourceBytes
    ) {
        return doGenerate(project, camera, snapshotDate, force, importedImage, importedSourceBytes);
    }

    private CameraSnapshotAsset doGenerate(
            Project project,
            Camera camera,
            LocalDate snapshotDate,
            boolean force,
            DetectionImage importedImage,
            byte[] importedSourceBytes
    ) {
        CameraSnapshotSourceDecision sourceDecision = resolveSourceUseCase.resolve(project, camera, snapshotDate);
        var existing = snapshotRepository.findByCameraIdAndSnapshotDate(camera.getId(), snapshotDate).orElse(null);
        if (existing != null && existing.isFrozen() && !force) {
            return toResult(existing);
        }

        CameraSnapshotProfile profile = profileService.getOrCreate(camera.getId());
        String targetKey = snapshotKeyFactory.create(project, camera, snapshotDate, profile.targetFormat());
        OffsetDateTime now = OffsetDateTime.now(clock).withOffsetSameInstant(ZoneOffset.UTC);
        if (!force && existing != null && !shouldRegenerate(existing, sourceDecision, targetKey)
                && existing.isFrozen() == sourceDecision.frozen()) {
                return toResult(existing);
            }


        TransformedSourcePayload sourcePayload = resolveTransformedSourcePayload(
                sourceDecision,
                camera,
                snapshotDate,
                profile,
                importedImage,
                importedSourceBytes
        );
        objectStorage.upload(sourcePayload.image().getBucket(), targetKey, sourcePayload.transformedImage().bytes(), sourcePayload.transformedImage().mediaType());

        CameraDailySnapshotEntity entity = existing == null ? new CameraDailySnapshotEntity() : existing;
        entity.setCameraId(camera.getId());
        entity.setSnapshotDate(snapshotDate);
        entity.setSourceImageId(sourcePayload.image().getId());
        entity.setBucket(sourcePayload.image().getBucket());
        entity.setKey(targetKey);
        entity.setMediaType(sourcePayload.transformedImage().mediaType());
        entity.setFrozen(sourceDecision.frozen());
        entity.setGeneratedAt(now);
        entity.setUpdatedAt(now);
        return toResult(snapshotRepository.save(entity));
    }

    private boolean shouldRegenerate(CameraDailySnapshotEntity existing, CameraSnapshotSourceDecision decision, String targetKey) {
        return existing.getSourceImageId() == null
                || decision.sourceImages().stream().noneMatch(image -> existing.getSourceImageId().equals(image.getId()))
                || existing.isFrozen() != decision.frozen()
                || !existing.getKey().equals(targetKey);
    }

    private TransformedSourcePayload resolveTransformedSourcePayload(
            CameraSnapshotSourceDecision decision,
            Camera camera,
            LocalDate snapshotDate,
            CameraSnapshotProfile profile,
            DetectionImage importedImage,
            byte[] importedSourceBytes
    ) {
        ExternalServiceException lastExternalFailure = null;
        ProcessingException lastProcessingFailure = null;
        for (ImageEntity candidate : decision.sourceImages()) {
            try {
                byte[] sourceBytes = matchesImportedCandidate(candidate, importedImage) && importedSourceBytes != null
                        ? importedSourceBytes
                        : objectStorage.download(candidate.getBucket(), candidate.getKey());
                WebImageTransformer.TransformedImage transformedImage = webImageTransformer.transform(sourceBytes, profile);
                return new TransformedSourcePayload(candidate, transformedImage);
            } catch (ExternalServiceException ex) {
                lastExternalFailure = ex;
                log.warn("Snapshot source download failed cameraId={} date={} imageId={} key={}, trying fallback candidate",
                        camera.getId(), snapshotDate, candidate.getId(), candidate.getKey());
            } catch (ProcessingException ex) {
                lastProcessingFailure = ex;
                log.warn("Snapshot source processing failed cameraId={} date={} imageId={} key={}, trying fallback candidate",
                        camera.getId(), snapshotDate, candidate.getId(), candidate.getKey());
            }
        }
        if (lastExternalFailure != null) {
            throw new ProcessingException("No accessible source image found for " + snapshotDate, lastExternalFailure);
        }
        if (lastProcessingFailure != null) {
            throw new ProcessingException("No transformable source image found for " + snapshotDate, lastProcessingFailure);
        }
        throw new ResourceNotFoundException("No source image found for " + snapshotDate);
    }

    private boolean matchesImportedCandidate(ImageEntity candidate, DetectionImage importedImage) {
        return importedImage != null && importedImage.getId() != null && importedImage.getId().equals(candidate.getId());
    }

    private record TransformedSourcePayload(ImageEntity image, WebImageTransformer.TransformedImage transformedImage) {
    }

    private CameraSnapshotAsset toResult(CameraDailySnapshotEntity entity) {
        return new CameraSnapshotAsset(
                entity.getCameraId(),
                entity.getSnapshotDate(),
                entity.getSourceImageId(),
                entity.getBucket(),
                entity.getKey(),
                entity.getMediaType(),
                entity.isFrozen(),
                entity.getUpdatedAt()
        );
    }
}
