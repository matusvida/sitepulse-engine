package com.sitepulse.engine.plan.infrastructure.external;

import com.sitepulse.engine.common.infrastructure.external.openai.OpenAiService;
import com.sitepulse.engine.plan.domain.model.MilestoneAssessment;
import com.sitepulse.engine.plan.domain.enums.MilestoneStatus;
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
                        milestone.getWeekNumber(),
                        milestone.getTitle() == null ? "Untitled" : milestone.getTitle(),
                        milestone.getDescription() == null ? "" : milestone.getDescription(),
                        milestone.getExpectedState() == null ? "" : milestone.getExpectedState()
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
                MilestoneStatus.fromValue(assessment.getStatus() == null ? "not_started" : assessment.getStatus()),
                assessment.getActualState() == null ? "" : assessment.getActualState()
        );
    }
}
