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
@Table(name = "detection_tracks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DetectionTrackEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @Column(name = "project_id")
    private Integer projectId;

    @Column(name = "camera_id")
    private Integer cameraId;

    @Column(name = "class_id", nullable = false)
    private Integer classId;

    @Column(name = "color_hint", length = 32)
    private String colorHint;

    @Column(name = "current_bbox_xyxy", columnDefinition = "text")
    private String currentBboxXyxy;

    @Column(name = "first_seen_image_id")
    private Integer firstSeenImageId;

    @Column(name = "last_seen_image_id")
    private Integer lastSeenImageId;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
