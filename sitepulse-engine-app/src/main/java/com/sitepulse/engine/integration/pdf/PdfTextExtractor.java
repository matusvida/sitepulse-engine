package com.sitepulse.engine.integration.pdf;

import com.sitepulse.engine.common.web.ApiException;
import java.io.IOException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class PdfTextExtractor {

    public String extract(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document).replace("\u0000", "").trim();
        } catch (IOException ex) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Could not extract text from PDF");
        }
    }
}
