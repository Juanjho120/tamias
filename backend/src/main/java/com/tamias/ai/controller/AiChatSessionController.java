package com.tamias.ai.controller;

import com.tamias.ai.dto.AiChatMessageResponse;
import com.tamias.ai.dto.AiChatSessionCreateRequest;
import com.tamias.ai.dto.AiChatSessionResponse;
import com.tamias.ai.dto.AiChatSessionSummaryResponse;
import com.tamias.ai.dto.AiChatSessionUpdateRequest;
import com.tamias.ai.service.AiChatSessionService;
import com.tamias.common.dto.PageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/chat-sessions")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
public class AiChatSessionController {

    private final AiChatSessionService chatSessionService;

    public AiChatSessionController(AiChatSessionService chatSessionService) {
        this.chatSessionService = chatSessionService;
    }

    @GetMapping
    public PageResponse<AiChatSessionSummaryResponse> findAll(
            @RequestParam(required = false) UUID propertyId,
            Pageable pageable
    ) {
        return chatSessionService.findAll(propertyId, pageable);
    }

    @GetMapping("/{sessionId}")
    public AiChatSessionResponse findById(@PathVariable UUID sessionId) {
        return chatSessionService.findById(sessionId);
    }

    @GetMapping("/{sessionId}/messages")
    public List<AiChatMessageResponse> findMessages(@PathVariable UUID sessionId) {
        return chatSessionService.findMessages(sessionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public AiChatSessionResponse create(@Valid @RequestBody AiChatSessionCreateRequest request) {
        return chatSessionService.create(request);
    }

    @PatchMapping("/{sessionId}/title")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public AiChatSessionSummaryResponse updateTitle(
            @PathVariable UUID sessionId,
            @Valid @RequestBody AiChatSessionUpdateRequest request
    ) {
        return chatSessionService.updateTitle(sessionId, request);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF')")
    public void delete(@PathVariable UUID sessionId) {
        chatSessionService.delete(sessionId);
    }
}
