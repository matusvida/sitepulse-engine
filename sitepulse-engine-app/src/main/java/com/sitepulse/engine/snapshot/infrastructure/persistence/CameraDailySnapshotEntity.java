package com.sitepulse.engine.snapshot.infrastructure.persistence;

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
@Table(name = "camera_daily_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CameraDailySnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @Column(name = "camera_id", nullable = false)
    private Integer cameraId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "source_image_id", nullable = false)
    private Integer sourceImageId;

    @Column(nullable = false, length = 256)
    private String bucket;

    @Column(name = "key", nullable = false, length = 1024)
    private String key;

    @Column(name = "media_type", nullable = false, length = 128)
    private String mediaType;

    @Column(name = "is_frozen", nullable = false)
    private boolean frozen;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
