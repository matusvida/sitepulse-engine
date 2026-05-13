package com.sitepulse.engine.report.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportImageRepository extends JpaRepository<ReportImageEntity, Integer> {

    List<ReportImageEntity> findByReportIdOrderByIdAsc(Integer reportId);
}
