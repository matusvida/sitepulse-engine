package com.sitepulse.engine.report.infrastructure.persistence;

import com.sitepulse.engine.report.domain.model.ProgressReport;
import com.sitepulse.engine.report.domain.port.ProgressReportCatalogRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProgressReportCatalogRepositoryAdapter implements ProgressReportCatalogRepository {

    private final ProgressReportRepository progressReportRepository;

    @Override
    public ProgressReport save(ProgressReport report) {
        ProgressReportEntity entity = report.getId() == null
                ? new ProgressReportEntity()
                : progressReportRepository.findById(report.getId()).orElseGet(ProgressReportEntity::new);
        entity.setProjectId(report.getProjectId());
        entity.setReportType(report.getReportType());
        entity.setContentMd(report.getContentMd());
        entity.setSummary(report.getSummary());
        entity.setDateRangeStart(report.getDateRangeStart());
        entity.setDateRangeEnd(report.getDateRangeEnd());
        entity.setImageCount(report.getImageCount());
        entity.setModelUsed(report.getModelUsed());
        entity.setCreatedAt(report.getCreatedAt());
        return toDomain(progressReportRepository.save(entity));
    }

    @Override
    public List<ProgressReport> findByProject(Integer projectId, int limit, int offset) {
        int page = Math.max(0, offset / Math.max(1, limit));
        return progressReportRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(page, limit)).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<ProgressReport> findByIdAndProject(Integer reportId, Integer projectId) {
        return progressReportRepository.findByIdAndProjectId(reportId, projectId).map(this::toDomain);
    }

    private ProgressReport toDomain(ProgressReportEntity entity) {
        return ProgressReport.restore(
                entity.getId(),
                entity.getProjectId(),
                entity.getReportType(),
                entity.getContentMd(),
                entity.getSummary(),
                entity.getDateRangeStart(),
                entity.getDateRangeEnd(),
                entity.getImageCount(),
                entity.getModelUsed(),
                entity.getCreatedAt()
        );
    }
}
