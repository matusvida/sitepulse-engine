package com.sitepulse.engine.report.infrastructure.external;

import com.sitepulse.engine.common.infrastructure.external.openai.OpenAiService;
import com.sitepulse.engine.common.infrastructure.external.openai.dto.OpenAiImagePayload;
import com.sitepulse.engine.report.domain.enums.ReportLanguage;
import com.sitepulse.engine.report.domain.model.ReportImageEvidence;
import com.sitepulse.engine.report.domain.port.ReportGenerator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiReportGenerator implements ReportGenerator {

    private final OpenAiService openAiService;

    @Override
    public String generate(List<ReportImageEvidence> imageData, String metricsContext, String milestonesContext, ReportLanguage language) {
        return openAiService.generateProgressReport(
                imageData.stream()
                        .map(image -> new OpenAiImagePayload(
                                image.capturedAt() != null ? image.capturedAt().toString() : image.date(),
                                image.base64Content()
                        ))
                        .toList(),
                metricsContext,
                milestonesContext,
                language
        );
    }
}
