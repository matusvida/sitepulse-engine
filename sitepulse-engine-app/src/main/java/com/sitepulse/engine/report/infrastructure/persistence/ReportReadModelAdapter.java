package com.sitepulse.engine.report.infrastructure.persistence;

import com.sitepulse.engine.report.application.result.ProgressReportResult;
import com.sitepulse.engine.report.domain.port.ReportReadModel;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportReadModelAdapter implements ReportReadModel {

    private final ProgressReportRepository progressReportRepository;

    @Override
    public List<ProgressReportResult> findByProject(Integer projectId, int limit, int offset) {
        int page = Math.max(0, offset / Math.max(1, limit));
        return progressReportRepository.findByProjectIdOrderByCreatedAtDesc(projectId, PageRequest.of(page, limit))
                .stream()
                .map(this::toResult)
                .toList();
    }

    private ProgressReportResult toResult(ProgressReportEntity entity) {
        return new ProgressReportResult(
                entity.getId(),
                entity.getProjectId(),
                entity.getReportType(),
                entity.getSummary(),
                entity.getDateRangeStart(),
                entity.getDateRangeEnd(),
                entity.getImageCount(),
                entity.getModelUsed(),
                entity.getCreatedAt(),
                entity.getContentMd()
        );
    }
}
