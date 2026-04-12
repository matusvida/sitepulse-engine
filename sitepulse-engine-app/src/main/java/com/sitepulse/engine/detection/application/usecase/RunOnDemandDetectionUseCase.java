package com.sitepulse.engine.detection.application.usecase;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.ValidationException;
import com.sitepulse.engine.detection.application.command.RunOnDemandDetectionCommand;
import com.sitepulse.engine.detection.application.result.DetectedObjectResult;
import com.sitepulse.engine.detection.application.result.DetectionOutcomeResult;
import com.sitepulse.engine.detection.application.service.DetectionExecutionResult;
import com.sitepulse.engine.detection.application.service.DetectionExecutionService;
import com.sitepulse.engine.detection.application.service.DetectionPersistenceService;
import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.DetectionOutcome;
import com.sitepulse.engine.detection.domain.model.DetectionTarget;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.detection.domain.port.DetectionImageRepository;
import com.sitepulse.engine.detection.domain.service.DetectionPostProcessor;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RunOnDemandDetectionUseCase {

    private final ObjectStorage objectStorage;
    private final DetectionExecutionService detectionExecutionService;
    private final DetectionPersistenceService detectionPersistenceService;
    private final DetectionImageRepository detectionImageRepository;
    private final CameraLookup cameraLookup;
    private final DetectionPostProcessor detectionPostProcessor;

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
        DetectionExecutionResult execution = null;
        try {
            byte[] imageBytes = objectStorage.download(target.bucket(), target.key());
            CameraRoiSettings cameraSettings = target.projectId() == null ? null : cameraLookup.findRoiSettings(target.projectId(), target.key());
            execution = detectionExecutionService.execute(image, imageBytes, cameraSettings);
            DetectionOutcome outcome = detectionPostProcessor.process(
                    target.bucket(),
                    target.key(),
                    imageBytes,
                    execution.inference(),
                    cameraSettings
            );
            List<DetectedObject> finalDetections = detectionPersistenceService.persistSuccess(image, outcome, execution);
            return toResult(outcome, finalDetections);
        } catch (RuntimeException ex) {
            detectionPersistenceService.persistFailure(
                    image,
                    execution == null ? null : execution.analysisRunId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    private DetectionOutcomeResult toResult(DetectionOutcome outcome, List<DetectedObject> detections) {
        return new DetectionOutcomeResult(
                outcome.modelVersion(),
                outcome.bucket(),
                outcome.key(),
                outcome.imageWidth(),
                outcome.imageHeight(),
                outcome.inferenceMs(),
                detections.stream()
                        .map(d -> new DetectedObjectResult(d.classId(), d.className(), d.score(), d.bboxXyxy(), d.inRoi(), d.trackId(), d.colorHint(), d.notes()))
                        .toList(),
                outcome.warnings(),
                outcome.skipped()
        );
    }

    private DetectionTarget resolveTarget(RunOnDemandDetectionCommand command) {
        if (command.s3Url() != null && !command.s3Url().isBlank()) {
            String raw = command.s3Url().trim();
            int schemeSeparator = raw.indexOf("://");
            if (schemeSeparator < 1 || schemeSeparator + 3 >= raw.length()) {
                throw new ValidationException("Invalid storage URL");
            }
            raw = raw.substring(schemeSeparator + 3);
            int slash = raw.indexOf('/');
            if (slash < 1 || slash + 1 >= raw.length()) {
                throw new ValidationException("Invalid storage URL");
            }
            return new DetectionTarget(raw.substring(0, slash), raw.substring(slash + 1), null, null);
        }
        if (command.key() == null || command.key().isBlank()) {
            throw new ValidationException("Either 'key' or 's3_url' must be provided");
        }
        return new DetectionTarget(
                command.bucket() == null || command.bucket().isBlank() ? objectStorage.defaultBucket() : command.bucket(),
                command.key(),
                null,
                null
        );
    }
}
