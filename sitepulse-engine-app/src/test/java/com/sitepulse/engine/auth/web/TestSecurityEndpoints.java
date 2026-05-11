package com.sitepulse.engine.auth.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestSecurityEndpoints {

    @GetMapping("/admin/users")
    ResponseEntity<String> adminUsers() {
        return ResponseEntity.ok("ok");
    }

    @GetMapping("/projects/{projectId}/activity/summary")
    @PreAuthorize("@projectAccessAuthorizationService.hasProjectAccess(authentication, #projectId)")
    ResponseEntity<String> projectSummary(@PathVariable Integer projectId) {
        return ResponseEntity.ok("project-ok");
    }
}
