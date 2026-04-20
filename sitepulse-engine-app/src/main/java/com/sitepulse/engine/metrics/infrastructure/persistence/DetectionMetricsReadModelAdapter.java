package com.sitepulse.engine.metrics.infrastructure.persistence;

import com.sitepulse.engine.detection.domain.enums.ImageStatus;
import com.sitepulse.engine.metrics.domain.model.ActivityHeatmapPoint;
import com.sitepulse.engine.metrics.domain.model.DetectionActivitySample;
import com.sitepulse.engine.metrics.domain.port.DetectionMetricsReadModel;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DetectionMetricsReadModelAdapter implements DetectionMetricsReadModel {

    private static final String PROJECT_ID_PARAM = "projectId";
    private static final String STATUS_PARAM = "status";

    private final EntityManager entityManager;

    @Override
    public List<LocalDate> findProcessedDates(Integer projectId, LocalDate cutoff) {
        Query query = entityManager.createNativeQuery("""
                SELECT DISTINCT DATE(captured_at)
                FROM images
                WHERE project_id = :projectId
                  AND status = :status
                  AND captured_at IS NOT NULL
                  AND DATE(captured_at) >= :cutoff
                ORDER BY DATE(captured_at)
                """);
        query.setParameter(PROJECT_ID_PARAM, projectId);
        query.setParameter(STATUS_PARAM, ImageStatus.DONE.name());
        query.setParameter("cutoff", cutoff);
        @SuppressWarnings("unchecked")
        List<Date> result = query.getResultList();
        return result.stream().map(Date::toLocalDate).toList();
    }

    @Override
    public List<LocalDate> findCompletedWeeks(Integer projectId, LocalDate sinceDate) {
        Query query = entityManager.createNativeQuery("""
                SELECT DISTINCT date_trunc('week', date)::date
                FROM daily_metrics
                WHERE project_id = :projectId AND date >= :since
                ORDER BY 1
                """);
        query.setParameter(PROJECT_ID_PARAM, projectId);
        query.setParameter("since", sinceDate);
        @SuppressWarnings("unchecked")
        List<Date> rows = query.getResultList();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return rows.stream()
                .map(Date::toLocalDate)
                .filter(weekStart -> weekStart.plusDays(6).isBefore(today))
                .toList();
    }

    @Override
    public List<DetectionActivitySample> findDetectionActivityForDay(Integer projectId, LocalDate targetDate) {
        Query query = entityManager.createNativeQuery("""
                SELECT c.class_name, c.class_group, d.image_id, i.captured_at
                FROM detections d
                JOIN detection_classes c ON d.class_id = c.id
                JOIN images i ON d.image_id = i.id
                WHERE d.project_id = :projectId
                  AND DATE(i.captured_at) = :targetDate
                  AND i.status = :status
                """);
        query.setParameter(PROJECT_ID_PARAM, projectId);
        query.setParameter("targetDate", targetDate);
        query.setParameter(STATUS_PARAM, ImageStatus.DONE.name());
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new DetectionActivitySample(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).intValue(),
                        toCapturedAt(row[3]),
                        targetDate
                ))
                .toList();
    }

    @Override
    public List<ActivityHeatmapPoint> getActivityHeatmap(Integer projectId) {
        Query query = entityManager.createNativeQuery("""
                SELECT EXTRACT(DOW FROM i.captured_at)::int AS dow,
                       EXTRACT(HOUR FROM i.captured_at)::int AS hr,
                       COUNT(d.id) AS cnt
                FROM detections d
                JOIN images i ON d.image_id = i.id
                WHERE d.project_id = :projectId AND i.captured_at IS NOT NULL
                GROUP BY dow, hr
                ORDER BY dow, hr
                """);
        query.setParameter(PROJECT_ID_PARAM, projectId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream()
                .map(row -> new ActivityHeatmapPoint(
                        (((Number) row[0]).intValue() - 1 + 7) % 7,
                        ((Number) row[1]).intValue(),
                        ((Number) row[2]).intValue()
                ))
                .toList();
    }

    private OffsetDateTime toCapturedAt(Object value) {
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        }
        if (value instanceof Instant instant) {
            return instant.atOffset(ZoneOffset.UTC);
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant().atOffset(ZoneOffset.UTC);
        }
        throw new IllegalStateException("Unsupported captured_at type: " + (value == null ? "null" : value.getClass().getName()));
    }
}
