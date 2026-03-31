package com.sitepulse.engine.plan.infrastructure.external;

import com.sitepulse.engine.integration.openai.OpenAiService;
import com.sitepulse.engine.plan.domain.model.MilestoneAssessment;
import com.sitepulse.engine.plan.domain.model.MilestoneStatus;
import com.sitepulse.engine.plan.domain.model.ParsedMilestone;
import com.sitepulse.engine.plan.domain.model.PlanMilestone;
import com.sitepulse.engine.plan.domain.port.PlanIntelligenceGateway;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiPlanIntelligenceGateway implements PlanIntelligenceGateway {

    private final OpenAiService openAiService;

    @Override
    public List<ParsedMilestone> parseMilestones(String rawPlanText) {
        return openAiService.parsePlanMilestones(rawPlanText).stream()
                .map(milestone -> new ParsedMilestone(
                        ((Number) milestone.getOrDefault("week_number", 0)).intValue(),
                        String.valueOf(milestone.getOrDefault("title", "Untitled")),
                        String.valueOf(milestone.getOrDefault("description", "")),
                        String.valueOf(milestone.getOrDefault("expected_state", ""))
                ))
                .toList();
    }

    @Override
    public MilestoneAssessment evaluateMilestone(PlanMilestone milestone, List<byte[]> evidenceImages) {
        var assessment = openAiService.evaluateMilestone(
                milestone.getTitle(),
                milestone.getExpectedState() == null ? "" : milestone.getExpectedState(),
                evidenceImages
        );
        return new MilestoneAssessment(
                MilestoneStatus.fromValue(String.valueOf(assessment.getOrDefault("status", "not_started"))),
                String.valueOf(assessment.getOrDefault("actual_state", ""))
        );
    }
}
