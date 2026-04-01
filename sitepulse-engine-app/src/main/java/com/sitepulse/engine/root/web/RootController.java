package com.sitepulse.engine.root.web;

import com.sitepulse.engine.http.root.api.RootApi;
import com.sitepulse.engine.http.root.dto.RootInfoView;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController implements RootApi {

    @Override
    @GetMapping("/")
    public RootInfoView root() {
        return new RootInfoView(
                "sitepulse-spring-app",
                "/swagger-ui.html",
                List.of("/health", "/detect", "/api/projects")
        );
    }
}
