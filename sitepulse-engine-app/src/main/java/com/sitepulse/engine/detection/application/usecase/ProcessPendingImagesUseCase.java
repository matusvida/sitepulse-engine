package com.sitepulse.engine.detection.application.usecase;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.SitePulseException;
import com.sitepulse.engine.detection.domain.model.CameraRoiSettings;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.DetectionOutcome;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.detection.domain.port.DetectionImageRepository;
import com.sitepulse.engine.detection.domain.service.DetectionPostProcessor;
import com.sitepulse.engine.detection.application.service.DetectionExecutionResult;
import com.sitepulse.engine.detection.application.service.DetectionExecutionService;
import com.sitepulse.engine.detection.application.service.DetectionPersistenceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProcessPendingImagesUseCase {

    private final ObjectStorage objectStorage;
    private final DetectionExecutionService detectionExecutionService;
    private final DetectionImageRepository detectionImageRepository;
    private final CameraLookup cameraLookup;
    private final DetectionPostProcessor detectionPostProcessor;
    private final DetectionPersistenceService detectionPersistenceService;

    public void process(int limit) {
        List<DetectionImage> images = detectionImageRepository.claimPendingImages(limit);
        log.info("Claimed {} new images for detection processing", images.size());
        for (DetectionImage image : images) {
            DetectionExecutionResult execution = null;
            try {
                byte[] imageBytes = objectStorage.download(image.getBucket(), image.getKey());
                CameraRoiSettings cameraSettings = image.getProjectId() == null ? null : cameraLookup.findRoiSettings(image.getProjectId(), image.getKey());
                execution = detectionExecutionService.execute(image, imageBytes, cameraSettings);
                DetectionOutcome outcome = detectionPostProcessor.process(
                        image.getBucket(),
                        image.getKey(),
                        imageBytes,
                        execution.inference(),
                        cameraSettings
                );
                detectionPersistenceService.persistSuccess(image, outcome, execution);
                log.info("Detection completed for imageId={} key={}", image.getId(), image.getKey());
            } catch (RuntimeException ex) {
                detectionPersistenceService.persistFailure(
                        image,
                        execution == null ? null : execution.analysisRunId(),
                        ex.getMessage()
                );
                log.error("Detection failed for imageId={} key={} reason={}", image.getId(), image.getKey(), ex.getMessage(), ex);
            }
        }
    }
}
