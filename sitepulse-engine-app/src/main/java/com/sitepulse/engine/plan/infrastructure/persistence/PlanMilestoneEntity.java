package com.sitepulse.engine.plan.infrastructure.persistence;

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
@Table(name = "plan_milestones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PlanMilestoneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Integer id;

    @Column(name = "plan_id", nullable = false)
    private Integer planId;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "week_number", nullable = false)
    @ToString.Include
    private Integer weekNumber;

    @Column(nullable = false, length = 512)
    @ToString.Include
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "expected_state", columnDefinition = "text")
    private String expectedState;

    @Column(name = "actual_state", columnDefinition = "text")
    private String actualState;

    @Column(length = 32)
    private String status;

    @Column(name = "checked_at")
    private OffsetDateTime checkedAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
