package com.sitepulse.engine.plan.infrastructure.persistence;

import com.sitepulse.engine.plan.domain.PlanMilestoneEntity;
import com.sitepulse.engine.plan.domain.model.MilestoneStatus;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;
import com.sitepulse.engine.plan.domain.port.PlanMilestoneCatalogRepository;
import com.sitepulse.engine.plan.persistence.PlanMilestoneRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PlanMilestoneCatalogRepositoryAdapter implements PlanMilestoneCatalogRepository {

    private final PlanMilestoneRepository planMilestoneRepository;

    @Override
    public List<PlanMilestone> findByPlanId(Integer planId) {
        return planMilestoneRepository.findByPlanIdOrderByWeekNumberAsc(planId).stream()
                .map(PlanMilestoneEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<PlanMilestone> findByPlanIdAndStatusNot(Integer planId, MilestoneStatus status) {
        return planMilestoneRepository.findByPlanIdAndStatusNotOrderByWeekNumberAsc(planId, status.toPersistenceValue()).stream()
                .map(PlanMilestoneEntityMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<PlanMilestone> findByIdAndProjectId(Integer milestoneId, Integer projectId) {
        return planMilestoneRepository.findByIdAndProjectId(milestoneId, projectId).map(PlanMilestoneEntityMapper::toDomain);
    }

    @Override
    public PlanMilestone save(PlanMilestone milestone) {
        PlanMilestoneEntity entity = milestone.getId() == null
                ? new PlanMilestoneEntity()
                : planMilestoneRepository.findById(milestone.getId()).orElseGet(PlanMilestoneEntity::new);
        entity.setPlanId(milestone.getPlanId());
        entity.setProjectId(milestone.getProjectId());
        entity.setWeekNumber(milestone.getWeekNumber());
        entity.setTitle(milestone.getTitle());
        entity.setDescription(milestone.getDescription());
        entity.setExpectedState(milestone.getExpectedState());
        entity.setActualState(milestone.getActualState());
        entity.setStatus(milestone.getStatus().toPersistenceValue());
        entity.setCheckedAt(milestone.getCheckedAt());
        entity.setCreatedAt(milestone.getCreatedAt());
        return PlanMilestoneEntityMapper.toDomain(planMilestoneRepository.save(entity));
    }
}
