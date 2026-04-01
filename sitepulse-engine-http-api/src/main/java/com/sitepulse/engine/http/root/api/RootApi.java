package com.sitepulse.engine.http.root.api;

import com.sitepulse.engine.http.root.dto.RootInfoView;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Root")
@RequestMapping
public interface RootApi {

    @Operation(summary = "Get service info")
    @GetMapping("/")
    RootInfoView root();
}
