package com.sitepulse.engine.root.web;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "sitepulse-spring-app",
                "docs", "/swagger-ui.html",
                "endpoints", List.of("/health", "/detect", "/api/projects")
        );
    }
}
