package com.sitepulse.engine.plan.infrastructure.external;

import com.sitepulse.engine.common.exception.ProcessingException;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class PdfTextExtractor {

    public String extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).replace("\u0000", "").trim();
        } catch (IOException ex) {
            throw new ProcessingException("Could not extract text from PDF", ex);
        }
    }
}
