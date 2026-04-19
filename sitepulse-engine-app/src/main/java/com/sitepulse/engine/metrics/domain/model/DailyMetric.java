package com.sitepulse.engine.metrics.domain.model;

import com.sitepulse.engine.metrics.domain.enums.DailyActivityStatus;
import com.sitepulse.engine.metrics.domain.enums.DailyObservationConfidence;
import com.sitepulse.engine.metrics.domain.enums.DailyWeatherStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class DailyMetric {

    private final Integer id;
    private final Integer projectId;

    @EqualsAndHashCode.Include
    @ToString.Include
    private final LocalDate date;

    private Integer peopleCount;
    private Integer vehicleCount;
    private Double activeHours;
    private DailyActivityStatus activityStatus;
    private DailyObservationConfidence activityConfidence;
    private DailyWeatherStatus weatherStatus;
    private Boolean weatherImpacted;
    private List<String> reasonCodes;
    private String summaryNote;
    private final OffsetDateTime createdAt;

    public static DailyMetric create(Integer projectId, LocalDate date, OffsetDateTime createdAt) {
        return new DailyMetric(
                null,
                projectId,
                date,
                0,
                0,
                0.0,
                DailyActivityStatus.UNKNOWN,
                DailyObservationConfidence.LOW,
                DailyWeatherStatus.UNCLEAR,
                false,
                List.of(),
                null,
                createdAt
        );
    }

    public static DailyMetric restore(
            Integer id,
            Integer projectId,
            LocalDate date,
            Integer peopleCount,
            Integer vehicleCount,
            Double activeHours,
            DailyActivityStatus activityStatus,
            DailyObservationConfidence activityConfidence,
            DailyWeatherStatus weatherStatus,
            Boolean weatherImpacted,
            List<String> reasonCodes,
            String summaryNote,
            OffsetDateTime createdAt
    ) {
        return new DailyMetric(
                id,
                projectId,
                date,
                peopleCount,
                vehicleCount,
                activeHours,
                activityStatus,
                activityConfidence,
                weatherStatus,
                weatherImpacted,
                reasonCodes == null ? List.of() : List.copyOf(reasonCodes),
                summaryNote,
                createdAt
        );
    }

    public void updateCounts(int peopleCount, int vehicleCount, double activeHours) {
        if (peopleCount < 0 || vehicleCount < 0 || activeHours < 0) {
            throw new IllegalArgumentException("Metric counts must not be negative");
        }
        this.peopleCount = peopleCount;
        this.vehicleCount = vehicleCount;
        this.activeHours = activeHours;
    }

    public void updateAssessment(
            DailyActivityStatus activityStatus,
            DailyObservationConfidence activityConfidence,
            DailyWeatherStatus weatherStatus,
            boolean weatherImpacted,
            List<String> reasonCodes,
            String summaryNote
    ) {
        this.activityStatus = activityStatus == null ? DailyActivityStatus.UNKNOWN : activityStatus;
        this.activityConfidence = activityConfidence == null ? DailyObservationConfidence.LOW : activityConfidence;
        this.weatherStatus = weatherStatus == null ? DailyWeatherStatus.UNCLEAR : weatherStatus;
        this.weatherImpacted = weatherImpacted;
        this.reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
        this.summaryNote = summaryNote;
    }
}
