package com.sitepulse.engine.alert.infrastructure.event;

import com.sitepulse.engine.alert.application.command.CreateAlertCommand;
import com.sitepulse.engine.alert.application.usecase.CreateAlertUseCase;
import com.sitepulse.engine.alert.domain.enums.AlertSeverity;
import com.sitepulse.engine.plan.domain.event.MilestoneDelayedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MilestoneDelayedEventHandler {

    private final CreateAlertUseCase createAlertUseCase;

    @EventListener
    public void onMilestoneDelayed(MilestoneDelayedEvent event) {
        createAlertUseCase.create(new CreateAlertCommand(
                event.getProjectId(),
                "schedule",
                AlertSeverity.HIGH,
                "Schedule delay: Week " + event.getWeekNumber() + " - " + event.getTitle(),
                "Milestone is behind schedule. Current assessment: " + event.getActualState(),
                java.util.List.of("Review milestone expectations", "Reallocate resources", "Schedule a site visit")
        ));
    }
}
