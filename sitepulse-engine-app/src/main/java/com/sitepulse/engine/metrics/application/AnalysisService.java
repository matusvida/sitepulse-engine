package com.sitepulse.engine.metrics.application;

import com.sitepulse.engine.alert.application.AlertService;
import com.sitepulse.engine.detection.domain.ImageStatus;
import com.sitepulse.engine.detection.domain.DetectionEntity;
import com.sitepulse.engine.detection.domain.ImageEntity;
import com.sitepulse.engine.detection.persistence.DetectionRepository;
import com.sitepulse.engine.detection.persistence.ImageRepository;
import com.sitepulse.engine.metrics.domain.DailyMetricEntity;
import com.sitepulse.engine.metrics.domain.WeeklyMetricEntity;
import com.sitepulse.engine.metrics.persistence.DailyMetricRepository;
import com.sitepulse.engine.metrics.persistence.WeeklyMetricRepository;
import com.sitepulse.engine.project.domain.ProjectEntity;
import com.sitepulse.engine.project.persistence.ProjectRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalysisService {

    private static final List<String> VEHICLE_CLASSES = List.of("car", "truck", "bus");
    private static final List<String> PERSON_CLASSES = List.of("person");

    private final DailyMetricRepository dailyMetricRepository;
    private final WeeklyMetricRepository weeklyMetricRepository;
    private final ProjectRepository projectRepository;
    private final AlertService alertService;
    private final EntityManager entityManager;

    public AnalysisService(
            DailyMetricRepository dailyMetricRepository,
            WeeklyMetricRepository weeklyMetricRepository,
            ProjectRepository projectRepository,
            AlertService alertService,
            EntityManager entityManager
    ) {
        this.dailyMetricRepository = dailyMetricRepository;
        this.weeklyMetricRepository = weeklyMetricRepository;
        this.projectRepository = projectRepository;
        this.alertService = alertService;
        this.entityManager = entityManager;
    }

    @Transactional
    public Map<String, Object> runAnalysisForProject(Integer projectId, int lookbackDays) {
        int daysProcessed = 0;
        LocalDate cutoff = LocalDate.now(ZoneOffset.UTC).minusDays(lookbackDays);
        for (LocalDate date : datesToProcess(projectId, cutoff)) {
            if (aggregateDay(projectId, date)) {
                daysProcessed++;
            }
        }

        int weeksProcessed = 0;
        for (LocalDate weekStart : completedWeeks(projectId, cutoff.minusDays(7))) {
            if (rollupWeek(projectId, weekStart)) {
                weeksProcessed++;
            }
        }

        generateAlerts(projectId);
        return Map.of(
                "projectId", projectId,
                "daysProcessed", daysProcessed,
                "weeksProcessed", weeksProcessed,
                "lookbackDays", lookbackDays
        );
    }

    @Transactional
    public void runAnalysis() {
        for (ProjectEntity project : projectRepository.findAll()) {
            runAnalysisForProject(project.getId(), 7);
        }
    }

    public List<Map<String, Object>> dailyMetrics(Integer projectId, int days) {
        return dailyMetricRepository.findByProjectIdAndDateGreaterThanEqualOrderByDateAsc(projectId, LocalDate.now(ZoneOffset.UTC).minusDays(days)).stream()
                .map(row -> {
                    Map<String, Object> payload = new HashMap<>();
                    payload.put("date", row.getDate().toString());
                    payload.put("peopleCount", row.getPeopleCount() == null ? 0 : row.getPeopleCount());
                    payload.put("vehicleCount", row.getVehicleCount() == null ? 0 : row.getVehicleCount());
                    payload.put("activeHours", row.getActiveHours() == null ? 0.0 : row.getActiveHours());
                    return payload;
                })
                .toList();
    }

    public List<Map<String, Object>> weeklyMetrics(Integer projectId, int weeks) {
        List<WeeklyMetricEntity> rows = new ArrayList<>(weeklyMetricRepository.findByProjectIdOrderByWeekStartDesc(projectId, PageRequest.of(0, weeks)));
        java.util.Collections.reverse(rows);
        return rows.stream().map(row -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("weekStart", row.getWeekStart().toString());
            payload.put("progressDelta", row.getProgressDelta() == null ? 0.0 : row.getProgressDelta());
            payload.put("activityIndex", row.getActivityIndex() == null ? 0.0 : row.getActivityIndex());
            payload.put("activeHours", row.getActiveHours() == null ? 0.0 : row.getActiveHours());
            payload.put("riskLevel", row.getRiskLevel() == null ? "Low" : row.getRiskLevel());
            return payload;
        }).toList();
    }

    public List<Map<String, Object>> activityHeatmap(Integer projectId) {
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
        query.setParameter("projectId", projectId);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        return rows.stream().map(row -> {
            Map<String, Object> payload = new HashMap<>();
            payload.put("dayOfWeek", (((Number) row[0]).intValue() - 1 + 7) % 7);
            payload.put("hour", ((Number) row[1]).intValue());
            payload.put("count", ((Number) row[2]).intValue());
            return payload;
        }).toList();
    }

    private List<LocalDate> datesToProcess(Integer projectId, LocalDate cutoff) {
        Query query = entityManager.createNativeQuery("""
                SELECT DISTINCT DATE(captured_at)
                FROM images
                WHERE project_id = :projectId
                  AND status = :status
                  AND captured_at IS NOT NULL
                  AND DATE(captured_at) >= :cutoff
                ORDER BY DATE(captured_at)
                """);
        query.setParameter("projectId", projectId);
        query.setParameter("status", ImageStatus.DONE.name());
        query.setParameter("cutoff", cutoff);
        @SuppressWarnings("unchecked")
        List<java.sql.Date> result = query.getResultList();
        return result.stream().map(java.sql.Date::toLocalDate).toList();
    }

    private List<LocalDate> completedWeeks(Integer projectId, LocalDate since) {
        Query query = entityManager.createNativeQuery("""
                SELECT DISTINCT date_trunc('week', date)::date
                FROM daily_metrics
                WHERE project_id = :projectId AND date >= :since
                ORDER BY 1
                """);
        query.setParameter("projectId", projectId);
        query.setParameter("since", since);
        @SuppressWarnings("unchecked")
        List<java.sql.Date> rows = query.getResultList();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return rows.stream()
                .map(java.sql.Date::toLocalDate)
                .filter(weekStart -> weekStart.plusDays(6).isBefore(today))
                .toList();
    }

    private boolean aggregateDay(Integer projectId, LocalDate targetDate) {
        Query query = entityManager.createNativeQuery("""
                SELECT d.class_name, d.image_id, i.captured_at
                FROM detections d
                JOIN images i ON d.image_id = i.id
                WHERE d.project_id = :projectId
                  AND DATE(i.captured_at) = :targetDate
                  AND i.status = :status
                """);
        query.setParameter("projectId", projectId);
        query.setParameter("status", ImageStatus.DONE.name());
        query.setParameter("targetDate", targetDate);
        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) {
            return false;
        }
        Map<Integer, Integer> peoplePerImage = new HashMap<>();
        Map<Integer, Integer> vehiclePerImage = new HashMap<>();
        Map<Integer, Integer> hoursWithDetections = new HashMap<>();
        for (Object[] row : rows) {
            String className = (String) row[0];
            Integer imageId = ((Number) row[1]).intValue();
            OffsetDateTime capturedAt = ((java.sql.Timestamp) row[2]).toInstant().atOffset(ZoneOffset.UTC);
            if (PERSON_CLASSES.contains(className)) {
                peoplePerImage.merge(imageId, 1, Integer::sum);
            } else if (VEHICLE_CLASSES.contains(className)) {
                vehiclePerImage.merge(imageId, 1, Integer::sum);
            }
            hoursWithDetections.merge(capturedAt.getHour(), 1, Integer::sum);
        }
        int peopleCount = peoplePerImage.values().stream().max(Integer::compareTo).orElse(0);
        int vehicleCount = vehiclePerImage.values().stream().max(Integer::compareTo).orElse(0);
        double activeHours = hoursWithDetections.values().stream().filter(count -> count >= 3).count();
        DailyMetricEntity entity = dailyMetricRepository.findByProjectIdAndDate(projectId, targetDate)
                .orElse(DailyMetricEntity.builder().projectId(projectId).date(targetDate).createdAt(OffsetDateTime.now(ZoneOffset.UTC)).build());
        entity.setPeopleCount(peopleCount);
        entity.setVehicleCount(vehicleCount);
        entity.setActiveHours(activeHours);
        dailyMetricRepository.save(entity);
        return true;
    }

    private boolean rollupWeek(Integer projectId, LocalDate weekStart) {
        List<DailyMetricEntity> daily = dailyMetricRepository.findByProjectIdAndDateBetweenOrderByDateAsc(projectId, weekStart, weekStart.plusDays(6));
        if (daily.isEmpty()) {
            return false;
        }
        double totalActivity = daily.stream().mapToDouble(row -> (row.getPeopleCount() == null ? 0 : row.getPeopleCount()) + (row.getVehicleCount() == null ? 0 : row.getVehicleCount())).sum();
        double totalHours = daily.stream().mapToDouble(row -> row.getActiveHours() == null ? 0 : row.getActiveHours()).sum();
        double previousActivity = dailyMetricRepository.findByProjectIdAndDateBetweenOrderByDateAsc(projectId, weekStart.minusDays(7), weekStart.minusDays(1)).stream()
                .mapToDouble(row -> (row.getPeopleCount() == null ? 0 : row.getPeopleCount()) + (row.getVehicleCount() == null ? 0 : row.getVehicleCount()))
                .sum();
        double progressDelta = previousActivity > 0 ? ((totalActivity - previousActivity) / previousActivity) * 100.0 : (totalActivity > 0 ? 100.0 : 0.0);
        double maxActivity = dailyMetricRepository.findByProjectIdAndDateGreaterThanEqualOrderByDateAsc(projectId, LocalDate.of(2000, 1, 1)).stream()
                .collect(java.util.stream.Collectors.groupingBy(row -> row.getDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))))
                .values().stream()
                .mapToDouble(rows -> rows.stream().mapToDouble(row -> (row.getPeopleCount() == null ? 0 : row.getPeopleCount()) + (row.getVehicleCount() == null ? 0 : row.getVehicleCount())).sum())
                .max()
                .orElse(1.0);
        double activityIndex = Math.min(100.0, maxActivity == 0 ? 0 : (totalActivity / maxActivity) * 100.0);
        Double rollingAverage = weeklyMetricRepository.findAverageActivityBefore(projectId, weekStart);
        String riskLevel = "Low";
        if (rollingAverage != null && rollingAverage > 0) {
            double dropPercent = ((rollingAverage - activityIndex) / rollingAverage) * 100.0;
            if (dropPercent > 40) {
                riskLevel = "High";
            } else if (dropPercent > 20) {
                riskLevel = "Medium";
            }
        }
        WeeklyMetricEntity entity = weeklyMetricRepository.findByProjectIdAndWeekStart(projectId, weekStart)
                .orElse(WeeklyMetricEntity.builder().projectId(projectId).weekStart(weekStart).createdAt(OffsetDateTime.now(ZoneOffset.UTC)).build());
        entity.setProgressDelta(progressDelta);
        entity.setActivityIndex(activityIndex);
        entity.setActiveHours(totalHours);
        entity.setRiskLevel(riskLevel);
        weeklyMetricRepository.save(entity);
        return true;
    }

    private void generateAlerts(Integer projectId) {
        List<DailyMetricEntity> daily = dailyMetricRepository.findByProjectIdAndDateGreaterThanEqualOrderByDateAsc(projectId, LocalDate.now(ZoneOffset.UTC).minusDays(14));
        List<WeeklyMetricEntity> weekly = weeklyMetricRepository.findByProjectIdOrderByWeekStartDesc(projectId, PageRequest.of(0, 4));

        long consecutiveLow = daily.reversed().stream()
                .takeWhile(row -> (row.getPeopleCount() == null ? 0 : row.getPeopleCount()) + (row.getVehicleCount() == null ? 0 : row.getVehicleCount()) <= 2)
                .count();
        if (consecutiveLow >= 3) {
            alertService.createAlert(projectId, "stall", "high",
                    "No significant activity detected for " + consecutiveLow + " consecutive days",
                    "Total detections have been at or below 2 for the last " + consecutiveLow + " days.",
                    List.of("Verify with site manager if work has been paused", "Check material delivery schedule", "Review weather logs"));
        } else {
            alertService.autoResolve(projectId, "stall");
        }

        long consecutiveNegativeWeeks = weekly.stream().takeWhile(row -> row.getProgressDelta() != null && row.getProgressDelta() < 0).count();
        if (consecutiveNegativeWeeks >= 2) {
            alertService.createAlert(projectId, "schedule", consecutiveNegativeWeeks >= 3 ? "high" : "medium",
                    "Activity declining for " + consecutiveNegativeWeeks + " consecutive weeks",
                    "Progress delta has been negative for " + consecutiveNegativeWeeks + " consecutive weeks.",
                    List.of("Review resource allocation", "Check for blocking issues", "Consider schedule adjustments"));
        } else {
            alertService.autoResolve(projectId, "schedule");
        }
    }
}
