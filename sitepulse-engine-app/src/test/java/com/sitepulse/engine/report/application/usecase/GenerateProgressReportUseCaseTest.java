package com.sitepulse.engine.report.application.usecase;

import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import com.sitepulse.engine.report.application.ReportResultMapper;
import com.sitepulse.engine.report.application.service.DailyReportSummaryBuilder;
import com.sitepulse.engine.report.application.service.ReportEvidenceQueryService;
import com.sitepulse.engine.report.application.service.ReportCompositionService;
import com.sitepulse.engine.report.application.service.WeeklyReportSummaryBuilder;
import com.sitepulse.engine.report.domain.model.ProgressReport;
import com.sitepulse.engine.report.domain.port.ReportEvidenceImageProvider;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import com.sitepulse.engine.report.domain.port.ReportContextProvider;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenerateProgressReportUseCaseTest {

    @Test
    void generateAutomaticDailyReturnsExistingReportWhenPeriodAlreadyExists() {
        AtomicBoolean generatorCalled = new AtomicBoolean(false);
        ProgressReport existing = ProgressReport.restore(
                55,
                1,
                "daily",
                "automatic",
                "daily:2026-04-16",
                "medium",
                "content",
                "headline",
                "summary",
                LocalDate.of(2026, 4, 16),
                LocalDate.of(2026, 4, 16),
                4,
                4,
                "gpt-4o",
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        GenerateProgressReportUseCase useCase = new GenerateProgressReportUseCase(
                new ProjectLookupService(new ProjectCatalogRepository() {
                    @Override
                    public List<Project> findAll() {
                        return List.of();
                    }

                    @Override
                    public Optional<Project> findById(Integer projectId) {
                        return Optional.of(Project.restore(1, "Demo", "loc", "key", "Europe/Bratislava", OffsetDateTime.now(ZoneOffset.UTC)));
                    }

                    @Override
                    public Project save(Project project) {
                        return project;
                    }
                }),
                new ReportContextProvider() {
                    @Override
                    public String getMetricsSummary(Integer projectId, int days) {
                        return "";
                    }

                    @Override
                    public String getMilestoneSummary(Integer projectId) {
                        return "";
                    }
                },
                new ProgressReportCatalogRepository() {
                    @Override
                    public ProgressReport save(ProgressReport report) {
                        return report;
                    }

                    @Override
                    public List<ProgressReport> findByProject(Integer projectId, int limit, int offset) {
                        return List.of();
                    }

                    @Override
                    public Optional<ProgressReport> findByIdAndProject(Integer reportId, Integer projectId) {
                        return Optional.empty();
                    }

                    @Override
                    public Optional<ProgressReport> findByProjectAndPeriodKey(Integer projectId, String periodKey) {
                        return Optional.of(existing);
                    }
                },
                new DailyReportSummaryBuilder(null, null, null),
                new WeeklyReportSummaryBuilder(null, null),
                new ReportEvidenceQueryService(new ReportEvidenceImageProvider() {
                    @Override
                    public List<com.sitepulse.engine.report.domain.model.ReportImageEvidence> gather(Integer projectId, LocalDate dateFrom, LocalDate dateTo, int maxImages) {
                        return List.of();
                    }
                }),
                new ReportCompositionService(
                        (projectId, dateFrom, dateTo, maxImages) -> List.of(),
                        (imageData, metricsContext, milestonesContext) -> {
                            generatorCalled.set(true);
                            return "content";
                        },
                        new ProgressReportCatalogRepository() {
                            @Override
                            public ProgressReport save(ProgressReport report) {
                                return report;
                            }

                            @Override
                            public List<ProgressReport> findByProject(Integer projectId, int limit, int offset) {
                                return List.of();
                            }

                            @Override
                            public Optional<ProgressReport> findByIdAndProject(Integer reportId, Integer projectId) {
                                return Optional.empty();
                            }

                            @Override
                            public Optional<ProgressReport> findByProjectAndPeriodKey(Integer projectId, String periodKey) {
                                return Optional.of(existing);
                            }
                        },
                        event -> {
                        },
                        Clock.systemUTC()
                ),
                new ReportResultMapper()
        );

        var result = useCase.generateAutomaticDaily(1, LocalDate.of(2026, 4, 16));

        assertTrue(result.isPresent());
        assertEquals("daily", result.get().getReportType());
        assertEquals("automatic", result.get().getGenerationOrigin());
        assertEquals(55, result.get().getId());
        assertTrue(!generatorCalled.get());
    }
}
