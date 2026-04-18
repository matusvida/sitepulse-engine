package com.sitepulse.engine.detection.infrastructure.persistence;

import com.sitepulse.engine.detection.domain.enums.ImageStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(name = "images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @Column(nullable = false, length = 256)
    private String bucket;

    @Column(name = "key", nullable = false, length = 1024)
    @ToString.Include
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImageStatus status;

    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "camera_id")
    private Integer cameraId;

    @Column(name = "captured_at")
    private OffsetDateTime capturedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "weather_note", length = 64)
    private String weatherNote;

    @Column(name = "evidence_activity_score")
    private Double evidenceActivityScore;

    @Column(name = "evidence_change_score")
    private Double evidenceChangeScore;

    @Column(name = "evidence_quality_score")
    private Double evidenceQualityScore;

    @Column(name = "evidence_overall_score")
    private Double evidenceOverallScore;

    @Column(name = "evidence_summary", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private String evidenceSummary;
}
