package com.sitepulse.engine.report.application.usecase;

import com.sitepulse.engine.project.domain.port.ProjectCatalogRepository;
import java.time.Clock;
import java.time.ZonedDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RunScheduledDailyReportUseCase {

    private final ProjectCatalogRepository projectCatalogRepository;
    private final GenerateProgressReportUseCase generateProgressReportUseCase;
    private final Clock clock;

    public void run() {
        ZonedDateTime now = ZonedDateTime.now(clock);
        projectCatalogRepository.findAll().forEach(project -> {
            var reportDate = generateProgressReportUseCase.previousCompletedDay(project, now);
            log.info("Running scheduled daily report generation for projectId={} date={}", project.getId(), reportDate);
            generateProgressReportUseCase.generateAutomaticDaily(project.getId(), reportDate);
        });
    }
}
