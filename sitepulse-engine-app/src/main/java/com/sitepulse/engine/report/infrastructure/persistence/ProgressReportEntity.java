package com.sitepulse.engine.report.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "progress_reports")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProgressReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "report_type", nullable = false, length = 32)
    private String reportType;

    @Column(name = "generation_origin", nullable = false, length = 16)
    private String generationOrigin;

    @Column(name = "period_key", length = 64)
    private String periodKey;

    @Column(name = "confidence_level", nullable = false, length = 16)
    private String confidenceLevel;

    @Column(name = "content_md", columnDefinition = "text")
    private String contentMd;

    @Column(length = 255)
    private String headline;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "date_range_start")
    private LocalDate dateRangeStart;

    @Column(name = "date_range_end")
    private LocalDate dateRangeEnd;

    @Column(name = "image_count")
    private Integer imageCount;

    @Column(name = "evidence_image_count")
    private Integer evidenceImageCount;

    @Column(name = "model_used", length = 128)
    private String modelUsed;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
