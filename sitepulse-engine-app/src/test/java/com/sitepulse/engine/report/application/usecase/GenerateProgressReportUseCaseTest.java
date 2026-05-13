package com.sitepulse.engine.report.application.usecase;

import com.sitepulse.engine.detection.domain.model.DetectedObject;
import com.sitepulse.engine.detection.domain.model.StoredImage;
import com.sitepulse.engine.detection.domain.port.ProcessedImageReadModel;
import com.sitepulse.engine.detection.infrastructure.persistence.ImageRepository;
import com.sitepulse.engine.common.domain.enums.ImageFormat;
import com.sitepulse.engine.common.domain.port.ObjectStorage;
import com.sitepulse.engine.config.SitePulseProperties;
import com.sitepulse.engine.metrics.domain.enums.DailyActivityStatus;
import com.sitepulse.engine.metrics.domain.port.WeeklyMetricCatalogRepository;
import com.sitepulse.engine.project.application.ProjectLookupService;
import com.sitepulse.engine.project.domain.model.Project;
import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import com.sitepulse.engine.report.application.ReportResultMapper;
import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.application.service.DailyReportSummary;
import com.sitepulse.engine.report.application.service.DailyReportSummaryBuilder;
import com.sitepulse.engine.report.application.service.ReportArtifactPersistenceService;
import com.sitepulse.engine.report.application.service.ReportCompositionService;
import com.sitepulse.engine.report.application.service.ReportEvidenceQueryService;
import com.sitepulse.engine.report.application.service.StoredReportImageAssetService;
import com.sitepulse.engine.report.application.service.StoredReportImageQueryService;
import com.sitepulse.engine.report.application.service.WeeklyReportSummaryBuilder;
import com.sitepulse.engine.report.domain.enums.ConfidenceLevel;
import com.sitepulse.engine.report.domain.enums.WeatherSummary;
import com.sitepulse.engine.report.domain.model.ProgressReport;
import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import com.sitepulse.engine.report.domain.port.ReportContextProvider;
import com.sitepulse.engine.report.domain.port.ReportEvidenceImageProvider;
import com.sitepulse.engine.report.infrastructure.persistence.ReportImageRepository;
import com.sitepulse.engine.snapshot.application.result.CameraSnapshotProfile;
import com.sitepulse.engine.snapshot.application.service.WebImageTransformer;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Base64;
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
                "sk",
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
                projectLookupService(),
                emptyProcessedImageReadModel(),
                emptyReportContextProvider(),
                reportRepositoryReturning(existing),
                new DailyReportSummaryBuilder(null, null, null),
                new WeeklyReportSummaryBuilder(null, null),
                new ReportEvidenceQueryService((projectId, dateFrom, dateTo, maxImages) -> List.of()),
                storedReportImageQueryService(),
                new ReportCompositionService(
                        (projectId, dateFrom, dateTo, maxImages) -> List.of(),
                        (imageData, metricsContext, milestonesContext, language) -> {
                            generatorCalled.set(true);
                            return "content";
                        },
                        reportArtifactPersistenceService(reportRepositoryReturning(existing)),
                        storedReportImageAssetService(),
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

    @Test
    void generateAutomaticWeeklyOnDemandUsesFirstCapturedDayForPartialFirstWeek() {
        OffsetDateTime createdAt = OffsetDateTime.of(2026, 4, 18, 9, 0, 0, 0, ZoneOffset.UTC);
        List<ProgressReport> savedReports = new ArrayList<>();
        GenerateProgressReportUseCase useCase = new GenerateProgressReportUseCase(
                projectLookupService(),
                new ProcessedImageReadModel() {
                    @Override
                    public List<OffsetDateTime> findSnapshotCapturedAtValues(Integer projectId) {
                        return List.of(
                                OffsetDateTime.of(2026, 4, 15, 8, 0, 0, 0, ZoneOffset.UTC),
                                OffsetDateTime.of(2026, 4, 17, 10, 0, 0, 0, ZoneOffset.UTC)
                        );
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
                        return List.of();
                    }

                    @Override
                    public List<StoredImage> findProcessedByProject(Integer projectId) {
                        return List.of();
                    }

                    @Override
                    public List<DetectedObject> findDetections(Integer imageId) {
                        return List.of();
                    }
                },
                emptyReportContextProvider(),
                new ProgressReportCatalogRepository() {
                    @Override
                    public ProgressReport save(ProgressReport report) {
                        savedReports.add(report);
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
                },
                new DailyReportSummaryBuilder(null, null, null),
                new WeeklyReportSummaryBuilder(
                        new DailyReportSummaryBuilder(null, null, null) {
                            @Override
                            public DailyReportSummary build(Integer projectId, LocalDate date, OffsetDateTime fromUtc, OffsetDateTime toUtc) {
                                return new DailyReportSummary(
                                        date,
                                        2,
                                        DailyActivityStatus.ACTIVE,
                                        WeatherSummary.CLEAR,
                                        ConfidenceLevel.MEDIUM,
                                        List.of("crane_tower"),
                                        List.of("first appearance of crane tower"),
                                        "daily"
                                );
                            }
                        },
                        new WeeklyMetricCatalogRepository() {
                            @Override
                            public Optional<com.sitepulse.engine.metrics.domain.model.WeeklyMetric> findByProjectAndWeekStart(Integer projectId, LocalDate weekStart) {
                                return Optional.empty();
                            }

                            @Override
                            public com.sitepulse.engine.metrics.domain.model.WeeklyMetric save(com.sitepulse.engine.metrics.domain.model.WeeklyMetric metric) {
                                return metric;
                            }

                            @Override
                            public List<com.sitepulse.engine.metrics.domain.model.WeeklyMetric> findLatest(Integer projectId, int limit) {
                                return List.of();
                            }

                            @Override
                            public BigDecimal findAverageActivityBefore(Integer projectId, LocalDate weekStart) {
                                return BigDecimal.ZERO;
                            }
                        }
                ),
                new ReportEvidenceQueryService(new ReportEvidenceImageProvider() {
                    @Override
                    public List<ReportImageEvidence> gather(Integer projectId, LocalDate dateFrom, LocalDate dateTo, int maxImages) {
                        return List.of(new ReportImageEvidence(
                                1,
                                dateFrom.toString(),
                                Base64.getEncoder().encodeToString("raw".getBytes()),
                                createdAt,
                                "bucket",
                                "key",
                                "url"
                        ));
                        }
                }),
                storedReportImageQueryService(),
                new ReportCompositionService(
                        (projectId, dateFrom, dateTo, maxImages) -> List.of(new ReportImageEvidence(
                                1,
                                dateFrom.toString(),
                                Base64.getEncoder().encodeToString("raw".getBytes()),
                                createdAt,
                                "bucket",
                                "key",
                                "url"
                        )),
                        (imageData, metricsContext, milestonesContext, language) -> "content",
                        reportArtifactPersistenceService(new ProgressReportCatalogRepository() {
                            @Override
                            public ProgressReport save(ProgressReport report) {
                                savedReports.add(report);
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
                        }),
                        storedReportImageAssetService(),
                        event -> {
                        },
                        Clock.systemUTC()
                ),
                new ReportResultMapper()
        );

        ProgressReportResult result = useCase.generateAutomaticWeeklyOnDemand(1, LocalDate.of(2026, 4, 17));

        assertEquals(LocalDate.of(2026, 4, 15), result.getDateRangeStart());
        assertEquals(LocalDate.of(2026, 4, 19), result.getDateRangeEnd());
        assertEquals("2026-04-15 - 2026-04-19", result.getPeriodLabel());
    }

    private ProjectLookupService projectLookupService() {
        return new ProjectLookupService(new ProjectCatalogRepository() {
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
        });
    }

    private ReportContextProvider emptyReportContextProvider() {
        return new ReportContextProvider() {
            @Override
            public String getMetricsSummary(Integer projectId, int days) {
                return "";
            }

            @Override
            public String getMilestoneSummary(Integer projectId) {
                return "";
            }
        };
    }

    private ProcessedImageReadModel emptyProcessedImageReadModel() {
        return new ProcessedImageReadModel() {
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
                return List.of();
            }

            @Override
            public List<StoredImage> findProcessedByProject(Integer projectId) {
                return List.of();
            }

            @Override
            public List<DetectedObject> findDetections(Integer imageId) {
                return List.of();
            }
        };
    }

    private ProgressReportCatalogRepository reportRepositoryReturning(ProgressReport report) {
        return new ProgressReportCatalogRepository() {
            @Override
            public ProgressReport save(ProgressReport value) {
                return value;
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
                return Optional.of(report);
            }
        };
    }

    private StoredReportImageQueryService storedReportImageQueryService() {
        return new StoredReportImageQueryService(
                reportImageRepository(),
                imageRepository(),
                objectStorage(),
                properties()
        );
    }

    private StoredReportImageAssetService storedReportImageAssetService() {
        return new StoredReportImageAssetService(
                objectStorage(),
                properties(),
                new WebImageTransformer() {
                    @Override
                    public TransformedImage transform(byte[] sourceBytes, CameraSnapshotProfile profile) {
                        return new TransformedImage(sourceBytes, "image/jpeg");
                    }
                }
        );
    }

    private ReportArtifactPersistenceService reportArtifactPersistenceService(ProgressReportCatalogRepository progressReportCatalogRepository) {
        return new ReportArtifactPersistenceService(progressReportCatalogRepository, reportImageRepository());
    }

    private ReportImageRepository reportImageRepository() {
        return proxy(ReportImageRepository.class, (proxy, method, args) -> switch (method.getName()) {
            case "findByReportIdOrderByIdAsc" -> List.of();
            case "saveAll" -> args[0];
            case "toString" -> "ReportImageRepositoryProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException("Unexpected method: " + method.getName());
        });
    }

    private ImageRepository imageRepository() {
        return proxy(ImageRepository.class, (proxy, method, args) -> switch (method.getName()) {
            case "findAllById" -> List.of();
            case "toString" -> "ImageRepositoryProxy";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            default -> throw new UnsupportedOperationException("Unexpected method: " + method.getName());
        });
    }

    private ObjectStorage objectStorage() {
        return new ObjectStorage() {
            @Override
            public byte[] download(String bucket, String key) {
                return new byte[0];
            }

            @Override
            public boolean exists(String bucket, String key) {
                return false;
            }

            @Override
            public void upload(String bucket, String key, InputStream data, long size, String contentType) {
            }

            @Override
            public String defaultBucket() {
                return "bucket";
            }

            @Override
            public String presign(String bucket, String key, Duration expiresAfter) {
                return "signed://" + bucket + "/" + key;
            }
        };
    }

    private SitePulseProperties properties() {
        return new SitePulseProperties(
                "postgresql://sitepulse:sitepulse@localhost:5432/sitepulse",
                "http://localhost:3000",
                "minio",
                "sitepulse-images",
                60,
                new SitePulseProperties.StorageProperties(
                        "http://minio:9000",
                        "http://localhost:9001",
                        "admin",
                        "password123",
                        "us-east-1"
                ),
                "yolov8x.pt",
                0.35,
                "{}",
                25.0,
                false,
                "roi_config.json",
                0.0,
                0,
                255,
                false,
                "0 0/10 * * * *",
                "0 5/10 * * * *",
                "openai",
                "0 0 2 * * *",
                3,
                "",
                "",
                "",
                "",
                "test-key",
                "gpt-4.1",
                52_428_800L,
                "http://python-yolo:8000",
                new SitePulseProperties.ImageWebSnapshotsProperties(false, 1920, 75, ImageFormat.WEBP, java.time.LocalTime.of(17, 0)),
                new SitePulseProperties.AuthProperties(
                        "http://localhost:3000",
                        "sitepulse_session",
                        false,
                        "Lax",
                        168,
                        72,
                        1,
                        null,
                        null,
                        new SitePulseProperties.MailProperties(
                                true,
                                "SitePulse",
                                "SitePulse <noreply@example.com>",
                                null,
                                new SitePulseProperties.ResendProperties(
                                        true,
                                        "https://api.resend.com",
                                        "re_test"
                                )
                        )
                )
        );
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }
}
