package com.sitepulse.engine.detection.application.usecase;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.ValidationException;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.detection.application.command.RunOnDemandDetectionCommand;
import com.sitepulse.engine.detection.application.result.DetectedObjectResult;
import com.sitepulse.engine.detection.application.result.DetectionOutcomeResult;
import com.sitepulse.engine.detection.application.service.DetectionExecutionResult;
import com.sitepulse.engine.detection.application.service.DetectionExecutionService;
import com.sitepulse.engine.detection.application.service.DetectionTrackingService;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.DetectionOutcome;
import com.sitepulse.engine.detection.domain.model.DetectionTarget;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
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
    private final DetectionExecutionService detectionExecutionService;
    private final DetectionTrackingService detectionTrackingService;
    private final DetectionImageRepository detectionImageRepository;
    private final DetectionRecordRepository detectionRecordRepository;
    private final CameraLookup cameraLookup;
    private final DetectionPostProcessor detectionPostProcessor;
    private final SitePulseProperties properties;

    public DetectionOutcomeResult run(RunOnDemandDetectionCommand command) {
        DetectionTarget target = resolveTarget(command);
        log.info("Running on-demand detection for bucket={} key={}", target.bucket(), target.key());
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DetectionImage image = detectionImageRepository.save(
                DetectionImage.createNew(
                        target.bucket(),
                        target.key(),
                        target.projectId(),
                        target.projectId() == null ? null : cameraLookup.findCameraIdByProjectAndKey(target.projectId(), target.key()),
                        null,
                        now
                )
        );
        image.markProcessing(now);
        image = detectionImageRepository.save(image);
        try {
            byte[] imageBytes = objectStorage.download(target.bucket(), target.key());
            DetectionExecutionResult execution = detectionExecutionService.execute(image, imageBytes);
            DetectionOutcome outcome = detectionPostProcessor.process(
                    target.bucket(),
                    target.key(),
                    imageBytes,
                    execution.inference(),
                    target.projectId() == null ? null : cameraLookup.findRoiSettings(target.projectId(), target.key())
            );

            if (!outcome.skipped()) {
                var tracked = detectionTrackingService.assignTracks(image, outcome.detections(), execution.provider());
                detectionRecordRepository.replaceDetections(image.getId(), target.projectId(), outcome.modelVersion(), execution.analysisRunId(), tracked);
            }

            image.markDone(OffsetDateTime.now(ZoneOffset.UTC));
            detectionImageRepository.save(image);
            return toResult(outcome);
        } catch (RuntimeException ex) {
            image.markFailed(OffsetDateTime.now(ZoneOffset.UTC));
            detectionImageRepository.save(image);
            throw ex;
        }
    }

    private DetectionOutcomeResult toResult(DetectionOutcome outcome) {
        return new DetectionOutcomeResult(
                outcome.modelVersion(),
                outcome.bucket(),
                outcome.key(),
                outcome.imageWidth(),
                outcome.imageHeight(),
                outcome.inferenceMs(),
                outcome.detections().stream()
                        .map(d -> new DetectedObjectResult(d.classId(), d.className(), d.score(), d.bboxXyxy(), d.inRoi(), d.trackId(), d.colorHint(), d.notes()))
                        .toList(),
                outcome.warnings(),
                outcome.skipped()
        );
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
