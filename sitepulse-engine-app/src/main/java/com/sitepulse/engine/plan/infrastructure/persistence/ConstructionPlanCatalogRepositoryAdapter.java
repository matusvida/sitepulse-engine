package com.sitepulse.engine.plan.infrastructure.persistence;

import com.sitepulse.engine.plan.domain.model.ConstructionPlan;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;
import com.sitepulse.engine.plan.domain.enums.PlanStatus;
import com.sitepulse.engine.plan.domain.port.ConstructionPlanCatalogRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@RequiredArgsConstructor
public class ConstructionPlanCatalogRepositoryAdapter implements ConstructionPlanCatalogRepository {

    private final ConstructionPlanRepository constructionPlanRepository;
    private final PlanMilestoneRepository planMilestoneRepository;

    @Override
    public ConstructionPlan save(ConstructionPlan plan) {
        return toDomain(constructionPlanRepository.save(toEntity(plan)));
    }

    @Override
    public Optional<ConstructionPlan> findLatestByProjectId(Integer projectId) {
        return constructionPlanRepository.findTopByProjectIdOrderByCreatedAtDesc(projectId).map(this::toDomain);
    }

    @Override
    public Optional<ConstructionPlan> findLatestByProjectIdAndStatus(Integer projectId, PlanStatus status) {
        return constructionPlanRepository.findTopByProjectIdAndStatusOrderByCreatedAtDesc(projectId, status.toPersistenceValue()).map(this::toDomain);
    }

    @Override
    @Transactional
    public ConstructionPlan saveUploadedPlan(ConstructionPlan plan, List<PlanMilestone> milestones) {
        ConstructionPlanEntity savedPlan = constructionPlanRepository.save(toEntity(plan));
        milestones.stream()
                .map(milestone -> PlanMilestoneEntityMapper.toEntity(milestone, savedPlan.getId()))
                .forEach(planMilestoneRepository::save);
        return toDomain(savedPlan);
    }

    private ConstructionPlanEntity toEntity(ConstructionPlan plan) {
        ConstructionPlanEntity entity = plan.getId() == null
                ? new ConstructionPlanEntity()
                : constructionPlanRepository.findById(plan.getId()).orElseGet(ConstructionPlanEntity::new);
        entity.setProjectId(plan.getProjectId());
        entity.setFilename(plan.getFilename());
        entity.setRawText(plan.getRawText());
        entity.setStatus(plan.getStatus().toPersistenceValue());
        entity.setCreatedAt(plan.getCreatedAt());
        entity.setUpdatedAt(plan.getUpdatedAt());
        return entity;
    }

    private ConstructionPlan toDomain(ConstructionPlanEntity entity) {
        return ConstructionPlan.restore(
                entity.getId(),
                entity.getProjectId(),
                entity.getFilename(),
                entity.getRawText(),
                PlanStatus.fromValue(entity.getStatus()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
