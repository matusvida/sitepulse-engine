package com.sitepulse.engine.project.application.command;

public record UpdateProjectCommand(Integer projectId, String name, String location, String storageKeyPrefix, String timezone) {
}
