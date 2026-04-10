package com.sitepulse.engine.project.application.command;

public record CreateProjectCommand(String name, String location, String storageKeyPrefix) {
}
