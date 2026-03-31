package com.sitepulse.engine.report.infrastructure.external;

import com.sitepulse.engine.integration.openai.OpenAiService;
import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import com.sitepulse.engine.report.domain.port.ReportGenerator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiReportGenerator implements ReportGenerator {

    private final OpenAiService openAiService;

    @Override
    public String generate(List<ReportImageEvidence> imageData, String metricsContext, String milestonesContext) {
        return openAiService.generateProgressReport(
                imageData.stream().map(image -> Map.<String, Object>of(
                        "date", image.date(),
                        "b64", image.base64Content()
                )).toList(),
                metricsContext,
                milestonesContext
        );
    }
}
