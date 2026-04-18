package com.sitepulse.engine.report.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgressReportRepository extends JpaRepository<ProgressReportEntity, Integer> {

    List<ProgressReportEntity> findByProjectIdOrderByCreatedAtDesc(Integer projectId, Pageable pageable);

    Optional<ProgressReportEntity> findByIdAndProjectId(Integer id, Integer projectId);

    Optional<ProgressReportEntity> findByProjectIdAndPeriodKey(Integer projectId, String periodKey);
}
