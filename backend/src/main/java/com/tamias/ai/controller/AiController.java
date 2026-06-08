package com.tamias.ai.controller;

import com.tamias.ai.dto.AiChatRequest;
import com.tamias.ai.dto.AiChatResponse;
import com.tamias.ai.dto.AiSearchRequest;
import com.tamias.ai.dto.AiSearchResponse;
import com.tamias.ai.service.AiRagService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiRagService aiRagService;

    public AiController(AiRagService aiRagService) {
        this.aiRagService = aiRagService;
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public AiSearchResponse search(@Valid @RequestBody AiSearchRequest request) {
        return aiRagService.search(request);
    }

    @PostMapping("/chat")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public AiChatResponse chat(@Valid @RequestBody AiChatRequest request) {
        return aiRagService.chat(request);
    }
}
