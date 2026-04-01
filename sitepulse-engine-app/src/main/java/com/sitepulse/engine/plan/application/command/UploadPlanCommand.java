package com.sitepulse.engine.plan.application.command;

public record UploadPlanCommand(Integer projectId, String filename, byte[] content) {
}
