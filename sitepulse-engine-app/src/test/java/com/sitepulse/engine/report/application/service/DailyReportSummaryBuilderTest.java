package com.sitepulse.engine.report.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitepulse.engine.common.util.JsonUtils;
import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.metrics.domain.enums.DailyActivityStatus;
import com.sitepulse.engine.metrics.domain.enums.DailyObservationConfidence;
import com.sitepulse.engine.metrics.domain.enums.DailyWeatherStatus;
import com.sitepulse.engine.metrics.domain.model.DailyMetric;
import com.sitepulse.engine.metrics.domain.port.DailyMetricCatalogRepository;
import com.sitepulse.engine.report.domain.enums.ConfidenceLevel;
import com.sitepulse.engine.report.domain.enums.WeatherSummary;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DailyReportSummaryBuilderTest {

    @Test
    void buildCreatesStructuredSummaryFromImagesAndDetections() {
        OffsetDateTime ts = OffsetDateTime.of(2026, 4, 16, 9, 0, 0, 0, ZoneOffset.UTC);
        JsonUtils jsonUtils = new JsonUtils(new ObjectMapper());
        DailyReportSummaryBuilder builder = new DailyReportSummaryBuilder(
                new ProcessedImageReadModel() {
                    @Override
                    public List<OffsetDateTime> findSnapshotCapturedAtValues(Integer projectId) {
                        return List.of();
                    }

                    @Override
                    public List<StoredImage> findRepresentativeSnapshots(Integer projectId) {
                        return List.of();
                    }

                    @Override
                    public Optional<StoredImage> findClosestSnapshot(Integer projectId, OffsetDateTime dayStart, OffsetDateTime dayEnd, OffsetDateTime midday) {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<StoredImage> findPreviousDoneImage(Integer projectId, Integer cameraId, OffsetDateTime capturedAt, Integer imageId) {
                        return Optional.empty();
                    }

                    @Override
                    public List<StoredImage> findDoneInRange(Integer projectId, OffsetDateTime from, OffsetDateTime to) {
                                return List.of(
                                new StoredImage(1, "bucket", "key-1", ts, "overcast", 7.0, 3.0, 5.0, 6.0,
                                        "{\"candidate_tags\":[\"loading_activity\"],\"first_appearance_flags\":[\"first_excavator\"],\"change_flags\":[\"more_truck\"]}"),
                                new StoredImage(2, "bucket", "key-2", ts.plusMinutes(20), "overcast", 6.0, 2.0, 5.0, 5.5,
                                        "{\"candidate_tags\":[\"upper_parking_activity\"]}")
                        );
                    }

                    @Override
                    public List<StoredImage> findProcessedByProject(Integer projectId) {
                        return List.of();
                    }

                    @Override
                    public List<DetectedObject> findDetections(Integer imageId) {
                        return imageId == 1
                                ? List.of(
                                new DetectedObject(1, "excavator", 0.9, List.of(1.0, 1.0, 2.0, 2.0), true, null, "yellow", "pit"),
                                new DetectedObject(2, "truck", 0.9, List.of(1.0, 1.0, 2.0, 2.0), true, null, "white", "site")
                        )
                                : List.of(new DetectedObject(2, "truck", 0.9, List.of(1.0, 1.0, 2.0, 2.0), true, null, "blue", "parking"));
                    }
                },
                new DailyMetricCatalogRepository() {
                    @Override
                    public Optional<DailyMetric> findByProjectAndDate(Integer projectId, LocalDate date) {
                        return Optional.of(DailyMetric.restore(
                                1,
                                projectId,
                                date,
                                2,
                                3,
                                4.5,
                                DailyActivityStatus.ACTIVE,
                                DailyObservationConfidence.MEDIUM,
                                DailyWeatherStatus.CLEAR_OR_NORMAL,
                                false,
                                List.of("movement_signals_present"),
                                "activity=active",
                                ts
                        ));
                    }

                    @Override
                    public DailyMetric save(DailyMetric metric) {
                        return metric;
                    }

                    @Override
                    public List<DailyMetric> findSince(Integer projectId, LocalDate sinceDate) {
                        return List.of();
                    }

                    @Override
                    public List<DailyMetric> findBetween(Integer projectId, LocalDate from, LocalDate to) {
                        return List.of();
                    }

                    @Override
                    public List<DailyMetric> findAllSince(Integer projectId, LocalDate sinceDate) {
                        return List.of();
                    }
                },
                jsonUtils
        );

        DailyReportSummary summary = builder.build(1, LocalDate.of(2026, 4, 16), ts.minusHours(1), ts.plusHours(6));

        assertEquals(2, summary.imageCount());
        assertEquals(DailyActivityStatus.ACTIVE, summary.activityStatus());
        assertEquals(WeatherSummary.OVERCAST, summary.weatherSummary());
        assertEquals(ConfidenceLevel.LOW, summary.confidenceLevel());
        assertTrue(summary.dominantClasses().contains("truck"));
        assertTrue(summary.contextText().contains("Structured Daily Summary"));
        assertTrue(summary.contextText().contains("loading activity"));
        assertTrue(summary.contextText().contains("first appearance of excavator"));
        assertTrue(summary.contextText().contains("increase in truck presence"));
    }
}
