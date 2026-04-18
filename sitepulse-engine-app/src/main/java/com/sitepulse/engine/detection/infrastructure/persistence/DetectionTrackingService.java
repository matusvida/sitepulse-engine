package com.sitepulse.engine.detection.infrastructure.persistence;

import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.detection.application.port.TrackAssignmentService;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.DetectionImage;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DetectionTrackingService implements TrackAssignmentService {

    private final DetectionTrackRepository detectionTrackRepository;
    private final JsonUtils jsonUtils;

    public List<DetectedObject> assignTracks(DetectionImage image, List<DetectedObject> detections) {
        if (image.getProjectId() == null || image.getCameraId() == null) {
            return detections;
        }
        List<DetectedObject> updated = new ArrayList<>();
        for (DetectedObject detection : detections) {
            Integer trackId = detection.trackId();
            DetectionTrackEntity track = trackId == null ? null : detectionTrackRepository.findById(trackId).orElse(null);
            if (track == null) {
                track = createTrack(image, detection);
                trackId = track.getId();
            } else {
                updateTrack(track, image, detection);
            }
            updated.add(new DetectedObject(
                    detection.classId(),
                    detection.className(),
                    detection.score(),
                    detection.bboxXyxy(),
                    detection.inRoi(),
                    trackId,
                    detection.colorHint(),
                    detection.notes()
            ));
        }
        return updated;
    }

    private DetectionTrackEntity createTrack(DetectionImage image, DetectedObject detection) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        DetectionTrackEntity track = DetectionTrackEntity.builder()
                .projectId(image.getProjectId())
                .cameraId(image.getCameraId())
                .classId(detection.classId())
                .firstSeenImageId(image.getId())
                .lastSeenImageId(image.getId())
                .currentBboxXyxy(jsonUtils.write(detection.bboxXyxy()))
                .colorHint(detection.colorHint())
                .active(true)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return detectionTrackRepository.save(track);
    }

    private void updateTrack(DetectionTrackEntity track, DetectionImage image, DetectedObject detection) {
        track.setLastSeenImageId(image.getId());
        track.setClassId(detection.classId());
        track.setCurrentBboxXyxy(jsonUtils.write(detection.bboxXyxy()));
        track.setColorHint(detection.colorHint());
        track.setActive(true);
        track.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        detectionTrackRepository.save(track);
        log.debug("Updated detection track id={} for imageId={}", track.getId(), image.getId());
    }
}
