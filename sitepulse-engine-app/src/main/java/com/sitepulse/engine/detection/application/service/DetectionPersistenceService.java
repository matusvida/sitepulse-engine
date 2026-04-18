package com.sitepulse.engine.detection.application.service;

import com.sitepulse.engine.detection.application.port.TrackAssignmentService;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import com.sitepulse.engine.detection.domain.model.DetectionOutcome;
import com.sitepulse.engine.detection.domain.port.DetectionImageRepository;
import com.sitepulse.engine.detection.domain.port.DetectionRecordRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DetectionPersistenceService {

    private final TrackAssignmentService detectionTrackingService;
    private final DetectionRecordRepository detectionRecordRepository;
    private final DetectionImageRepository detectionImageRepository;
    private final DetectionAnalysisRunService detectionAnalysisRunService;
    private final ImageEvidenceScoringService imageEvidenceScoringService;

    @Transactional
    public List<DetectedObject> persistSuccess(
            DetectionImage image,
            DetectionOutcome outcome,
            DetectionExecutionResult execution
    ) {
        List<DetectedObject> finalDetections = outcome.detections();
        if (!outcome.skipped()) {
            finalDetections = detectionTrackingService.assignTracks(image, outcome.detections());
            detectionRecordRepository.replaceDetections(
                    image.getId(),
                    image.getProjectId(),
                    outcome.modelVersion(),
                    execution.analysisRunId(),
                    finalDetections
            );
        }
        var previousImage = detectionImageRepository.findPreviousDone(image).orElse(null);
        var previousDetections = previousImage == null ? List.<DetectedObject>of() : detectionRecordRepository.findDetections(previousImage.getId());
        ImageEvidenceFeatures features = imageEvidenceScoringService.score(image, outcome, finalDetections, previousImage, previousDetections);
        image.applyAnalysisMetadata(
                features.weatherNote(),
                features.activityScore(),
                features.changeScore(),
                features.qualityScore(),
                features.overallScore(),
                features.summaryJson()
        );
        image.markDone(OffsetDateTime.now(ZoneOffset.UTC));
        detectionImageRepository.save(image);
        detectionAnalysisRunService.completeSuccess(
                execution.analysisRunId(),
                outcome.inferenceMs(),
                execution.rawResponse()
        );
        return finalDetections;
    }

    @Transactional
    public void persistFailure(DetectionImage image, Integer analysisRunId, String error) {
        image.markFailed(OffsetDateTime.now(ZoneOffset.UTC));
        detectionImageRepository.save(image);
        if (analysisRunId != null) {
            detectionAnalysisRunService.completeFailure(analysisRunId, error);
        }
    }
}
