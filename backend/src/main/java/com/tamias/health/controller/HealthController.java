package com.tamias.health.controller;

import com.tamias.health.dto.HealthResponse;
import java.time.OffsetDateTime;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public HealthResponse health() {
        return new HealthResponse(
                "UP",
                "tamias-api",
                OffsetDateTime.now()
        );
    }
}
