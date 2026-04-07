package com.sitepulse.engine.detection.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "detections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DetectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @Column(name = "image_id", nullable = false)
    private Integer imageId;

    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "model_version", length = 128)
    private String modelVersion;

    @Column(name = "class_id")
    private Integer classId;

    @Column
    private Double score;

    @Column(name = "bbox_xyxy", columnDefinition = "text")
    private String bboxXyxy;

    @Column(name = "track_id")
    private Integer trackId;

    @Column(name = "analysis_run_id")
    private Integer analysisRunId;

    @Column(name = "in_roi", length = 8)
    private String inRoi;

    @Column(name = "color_hint", length = 32)
    private String colorHint;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
