package com.sitepulse.engine.metrics.domain;

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
@Table(name = "weekly_metrics")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WeeklyMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "week_start", nullable = false)
    @ToString.Include
    private LocalDate weekStart;

    @Column(name = "progress_delta")
    private Double progressDelta;

    @Column(name = "activity_index")
    private Double activityIndex;

    @Column(name = "active_hours")
    private Double activeHours;

    @Column(name = "risk_level", length = 16)
    private String riskLevel;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
