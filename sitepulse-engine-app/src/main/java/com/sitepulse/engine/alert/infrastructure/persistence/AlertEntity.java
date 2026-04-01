package com.sitepulse.engine.alert.infrastructure.persistence;

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
@Table(name = "alerts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "type", length = 32)
    private String type;

    @Column(length = 16)
    private String severity;

    @Column(length = 16)
    private String status;

    @Column(columnDefinition = "text")
    @ToString.Include
    private String summary;

    @Column(columnDefinition = "text")
    private String details;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "recommended_actions", columnDefinition = "jsonb")
    private List<String> recommendedActions;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
