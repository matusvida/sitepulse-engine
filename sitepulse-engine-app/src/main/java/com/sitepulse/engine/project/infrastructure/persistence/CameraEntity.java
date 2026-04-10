package com.sitepulse.engine.project.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "cameras")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CameraEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(length = 256)
    @ToString.Include
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "roi_polygon", columnDefinition = "jsonb")
    private List<List<Double>> roiPolygon;

    @Column(name = "drop_outside")
    private Boolean dropOutside;

    @Column(name = "dropbox_path", length = 1024)
    private String dropboxPath;

    @Column(name = "key_prefix", length = 512)
    private String keyPrefix;

    @Column(name = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    private Integer imageHeight;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
