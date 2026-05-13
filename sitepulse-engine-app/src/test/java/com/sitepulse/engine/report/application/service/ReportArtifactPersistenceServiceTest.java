package com.sitepulse.engine.report.application.service;

import com.sitepulse.engine.report.domain.model.ProgressReport;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import com.sitepulse.engine.report.infrastructure.persistence.ReportImageEntity;
import com.sitepulse.engine.report.infrastructure.persistence.ReportImageRepository;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportArtifactPersistenceServiceTest {

    @Test
    void savePersistsReportAndPreparedImagesTogether() {
        List<ReportImageEntity> savedEntities = new ArrayList<>();
        ReportArtifactPersistenceService service = new ReportArtifactPersistenceService(
                progressReportCatalogRepository(),
                reportImageRepository(savedEntities)
        );
        ProgressReport report = ProgressReport.create(
                1,
                "weekly",
                "automatic",
                "weekly:2026-04-27",
                "high",
                "SK",
                "content",
                "headline",
                "summary",
                LocalDate.of(2026, 4, 27),
                LocalDate.of(2026, 5, 3),
                2,
                2,
                "gpt-4o",
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        ProgressReport saved = service.save(report, List.of(
                new StoredReportImageAssetService.PreparedReportImageAsset(10, "sitepulse-images", "reports/one.jpg"),
                new StoredReportImageAssetService.PreparedReportImageAsset(11, "sitepulse-images", "reports/two.jpg")
        ));

        assertEquals(77, saved.getId());
        assertEquals(2, savedEntities.size());
        assertEquals(77, savedEntities.get(0).getReportId());
        assertEquals(10, savedEntities.get(0).getImageId());
        assertEquals("reports/one.jpg", savedEntities.get(0).getImagePath());
        assertEquals(77, savedEntities.get(1).getReportId());
        assertEquals(11, savedEntities.get(1).getImageId());
        assertEquals("reports/two.jpg", savedEntities.get(1).getImagePath());
    }

    private ProgressReportCatalogRepository progressReportCatalogRepository() {
        return new ProgressReportCatalogRepository() {
            @Override
            public ProgressReport save(ProgressReport report) {
                return ProgressReport.restore(
                        77,
                        report.getProjectId(),
                        report.getReportType(),
                        report.getGenerationOrigin(),
                        report.getPeriodKey(),
                        report.getConfidenceLevel(),
                        report.getLanguage(),
                        report.getContentMd(),
                        report.getHeadline(),
                        report.getSummary(),
                        report.getDateRangeStart(),
                        report.getDateRangeEnd(),
                        report.getImageCount(),
                        report.getEvidenceImageCount(),
                        report.getModelUsed(),
                        report.getCreatedAt()
                );
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
            public Optional<ProgressReport> findByProjectAndPeriodKeyAndLanguage(Integer projectId, String periodKey, String language) {
                return Optional.empty();
            }
        };
    }

    private ReportImageRepository reportImageRepository(List<ReportImageEntity> savedEntities) {
        return (ReportImageRepository) Proxy.newProxyInstance(
                ReportImageRepository.class.getClassLoader(),
                new Class<?>[] {ReportImageRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "saveAll" -> {
                        @SuppressWarnings("unchecked")
                        List<ReportImageEntity> entities = (List<ReportImageEntity>) args[0];
                        savedEntities.clear();
                        savedEntities.addAll(entities);
                        yield entities;
                    }
                    case "toString" -> "ReportImageRepositoryProxy";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Unexpected method: " + method.getName());
                }
        );
    }
}
