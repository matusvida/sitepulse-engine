package com.sitepulse.engine.detection.application.usecase;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.ValidationException;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.application.command.RunOnDemandDetectionCommand;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.DetectionInference;
import com.sitepulse.engine.detection.domain.model.DetectionOutcome;
import com.sitepulse.engine.detection.domain.model.DetectionTarget;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.detection.domain.port.DetectionGateway;
import com.sitepulse.engine.detection.domain.port.DetectionImageRepository;
import com.sitepulse.engine.detection.domain.port.DetectionRecordRepository;
import com.sitepulse.engine.detection.domain.service.DetectionPostProcessor;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunOnDemandDetectionUseCase {

    private final ObjectStorage objectStorage;
    private final DetectionGateway detectionGateway;
    private final DetectionImageRepository detectionImageRepository;
    private final DetectionRecordRepository detectionRecordRepository;
    private final CameraLookup cameraLookup;
    private final DetectionPostProcessor detectionPostProcessor;
    private final SitePulseProperties properties;

    public DetectionOutcome run(RunOnDemandDetectionCommand command) {
        DetectionTarget target = resolveTarget(command);
        log.info("Running on-demand detection for bucket={} key={}", target.bucket(), target.key());
        byte[] imageBytes = objectStorage.download(target.bucket(), target.key());
        DetectionInference inference = detectionGateway.infer(imageBytes);
        DetectionOutcome outcome = detectionPostProcessor.process(
                target.bucket(),
                target.key(),
                imageBytes,
                inference,
                target.projectId() == null ? null : cameraLookup.findRoiSettings(target.projectId(), target.key())
        );
        if (!outcome.skipped()) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            DetectionImage image = detectionImageRepository.save(
                    DetectionImage.createDetected(
                            target.bucket(),
                            target.key(),
                            target.projectId(),
                            target.projectId() == null ? null : cameraLookup.findCameraIdByProjectAndKey(target.projectId(), target.key()),
                            null,
                            now
                    )
            );
            detectionRecordRepository.replaceDetections(image.getId(), target.projectId(), outcome.modelVersion(), outcome.detections());
        }
        return outcome;
    }

    private DetectionTarget resolveTarget(RunOnDemandDetectionCommand command) {
        if (command.s3Url() != null && !command.s3Url().isBlank()) {
            if (!command.s3Url().startsWith("s3://")) {
                throw new ValidationException("Invalid S3 URL");
            }
            String raw = command.s3Url().substring("s3://".length());
            int slash = raw.indexOf('/');
            if (slash < 0) {
                throw new ValidationException("Invalid S3 URL");
            }
            return new DetectionTarget(raw.substring(0, slash), raw.substring(slash + 1), null, null);
        }
        if (command.key() == null || command.key().isBlank()) {
            throw new ValidationException("Either 'key' or 's3_url' must be provided");
        }
        return new DetectionTarget(
                command.bucket() == null || command.bucket().isBlank() ? properties.minioBucketDefault() : command.bucket(),
                command.key(),
                null,
                null
        );
    }
}
