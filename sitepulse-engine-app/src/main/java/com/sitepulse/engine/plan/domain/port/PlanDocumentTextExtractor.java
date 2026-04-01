package com.sitepulse.engine.plan.domain.port;

public interface PlanDocumentTextExtractor {

    String extract(byte[] pdfBytes);
}
