package com.sitepulse.engine.plan.application.usecase;

import com.sitepulse.engine.plan.application.PlanResultMapper;
import com.sitepulse.engine.plan.application.result.PlanSummaryResult;
import com.sitepulse.engine.plan.domain.port.ConstructionPlanCatalogRepository;
import com.sitepulse.engine.plan.domain.port.PlanMilestoneCatalogRepository;
import com.sitepulse.engine.project.application.ProjectLookupService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetLatestPlanQuery {

    private final ProjectLookupService projectLookupService;
    private final ConstructionPlanCatalogRepository constructionPlanCatalogRepository;
    private final PlanMilestoneCatalogRepository planMilestoneCatalogRepository;
    private final PlanResultMapper planResultMapper;

    public Optional<PlanSummaryResult> get(Integer projectId) {
        projectLookupService.requireProject(projectId);
        return constructionPlanCatalogRepository.findLatestByProjectId(projectId)
                .map(plan -> planResultMapper.toSummaryResult(plan, planMilestoneCatalogRepository.findByPlanId(plan.getId())));
    }
}
