package com.sitepulse.engine.plan.application.usecase;

import com.sitepulse.engine.plan.application.PlanResultMapper;
import com.sitepulse.engine.plan.application.result.PlanMilestoneResult;
import com.sitepulse.engine.plan.domain.port.ConstructionPlanCatalogRepository;
import com.sitepulse.engine.plan.domain.port.PlanMilestoneCatalogRepository;
import com.sitepulse.engine.project.application.ProjectLookupService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ListPlanMilestonesQuery {

    private final ProjectLookupService projectLookupService;
    private final ConstructionPlanCatalogRepository constructionPlanCatalogRepository;
    private final PlanMilestoneCatalogRepository planMilestoneCatalogRepository;
    private final PlanResultMapper planResultMapper;

    public List<PlanMilestoneResult> list(Integer projectId) {
        projectLookupService.requireProject(projectId);
        return constructionPlanCatalogRepository.findLatestByProjectId(projectId)
                .map(plan -> planMilestoneCatalogRepository.findByPlanId(plan.getId()).stream()
                        .map(planResultMapper::toMilestoneResult)
                        .toList())
                .orElse(List.of());
    }
}
