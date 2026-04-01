package com.sitepulse.engine.plan.infrastructure.external;
import com.sitepulse.engine.plan.domain.port.PlanDocumentTextExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PdfPlanDocumentTextExtractor implements PlanDocumentTextExtractor {

    private final PdfTextExtractor pdfTextExtractor;

    @Override
    public String extract(byte[] pdfBytes) {
        return pdfTextExtractor.extract(pdfBytes);
    }
}
