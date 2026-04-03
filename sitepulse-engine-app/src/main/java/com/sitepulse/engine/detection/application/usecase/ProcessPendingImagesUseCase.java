package com.sitepulse.engine.detection.application.usecase;

import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.common.exception.SitePulseException;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.DetectionOutcome;
import com.sitepulse.engine.detection.domain.port.CameraLookup;
import com.sitepulse.engine.detection.domain.port.DetectionImageRepository;
import com.sitepulse.engine.detection.domain.port.DetectionRecordRepository;
import com.sitepulse.engine.detection.domain.service.DetectionPostProcessor;
import com.sitepulse.engine.detection.application.service.DetectionExecutionResult;
import com.sitepulse.engine.detection.application.service.DetectionExecutionService;
import com.sitepulse.engine.detection.application.service.DetectionTrackingService;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
    private final DetectionRecordRepository detectionRecordRepository;
    private final CameraLookup cameraLookup;
    private final DetectionPostProcessor detectionPostProcessor;
    private final DetectionTrackingService detectionTrackingService;

    public void process(int limit) {
        List<DetectionImage> images = detectionImageRepository.claimPendingImages(limit);
        log.info("Claimed {} new images for detection processing", images.size());
        for (DetectionImage image : images) {
            try {
                byte[] imageBytes = objectStorage.download(image.getBucket(), image.getKey());
                DetectionExecutionResult execution = detectionExecutionService.execute(image, imageBytes);
                DetectionOutcome outcome = detectionPostProcessor.process(
                        image.getBucket(),
                        image.getKey(),
                        imageBytes,
                        execution.inference(),
                        image.getProjectId() == null ? null : cameraLookup.findRoiSettings(image.getProjectId(), image.getKey())
                );
                image.markDone(OffsetDateTime.now(ZoneOffset.UTC));
                detectionImageRepository.save(image);
                if (!outcome.skipped()) {
                    var tracked = detectionTrackingService.assignTracks(image, outcome.detections(), execution.provider());
                    detectionRecordRepository.replaceDetections(
                            image.getId(),
                            image.getProjectId(),
                            outcome.modelVersion(),
                            execution.analysisRunId(),
                            tracked
                    );
                }
                log.info("Detection completed for imageId={} key={}", image.getId(), image.getKey());
            } catch (RuntimeException ex) {
                image.markFailed(OffsetDateTime.now(ZoneOffset.UTC));
                detectionImageRepository.save(image);
                log.error("Detection failed for imageId={} key={} reason={}", image.getId(), image.getKey(), ex.getMessage(), ex);
            }
        }
    }
}
