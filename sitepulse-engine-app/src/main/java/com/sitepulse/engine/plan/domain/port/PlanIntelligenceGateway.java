package com.sitepulse.engine.plan.domain.port;

import com.sitepulse.engine.plan.domain.model.MilestoneAssessment;
import com.sitepulse.engine.plan.domain.model.ParsedMilestone;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;
import java.util.List;

public interface PlanIntelligenceGateway {

    List<ParsedMilestone> parseMilestones(String rawPlanText);

    MilestoneAssessment evaluateMilestone(PlanMilestone milestone, List<byte[]> evidenceImages);
}
