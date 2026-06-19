package com.tamias.ai.service;

import com.tamias.ai.dto.AiChatMessageDebugResponse;
import com.tamias.ai.dto.AiToolDebugTrace;
import com.tamias.ai.entity.AiChatMessage;
import com.tamias.ai.entity.AiChatMessageDebug;
import com.tamias.ai.repository.AiChatMessageDebugRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiChatDebugTraceService {

    private final AiChatMessageDebugRepository debugRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public AiChatDebugTraceService(
            AiChatMessageDebugRepository debugRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService
    ) {
        this.debugRepository = debugRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public AiChatMessageDebug saveTrace(AiChatMessage assistantMessage, AiToolDebugTrace trace) {
        if (assistantMessage == null || trace == null) {
            return null;
        }

        AiChatMessageDebug debug = debugRepository
                .findByAiChatMessage_Id(assistantMessage.getId())
                .orElseGet(AiChatMessageDebug::new);

        debug.setAiChatMessage(assistantMessage);
        debug.setHandler(blankToNull(trace.handler()));
        debug.setToolName(blankToNull(trace.toolName()));
        debug.setToolNames(new ArrayList<>(trace.toolNames()));
        debug.setParams(new LinkedHashMap<>(trace.params()));
        debug.setRagUsed(Boolean.TRUE.equals(trace.ragUsed()));
        debug.setAnswerSource(trace.answerSource());
        debug.setRouteReason(truncate(blankToNull(trace.routeReason()), 500));
        debug.setFallbackReason(truncate(blankToNull(trace.fallbackReason()), 500));
        debug.setErrorMessage(blankToNull(trace.errorMessage()));

        return debugRepository.save(debug);
    }

    @Transactional(readOnly = true)
    public Optional<AiChatMessageDebugResponse> findDebugForMessageIfEnabled(AiChatMessage message) {
        if (message == null || !isDebugEnabledForCurrentUser()) {
            return Optional.empty();
        }
        return debugRepository.findByAiChatMessage_Id(message.getId()).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Optional<AiChatMessageDebugResponse> findDebugForMessageIfEnabled(java.util.UUID messageId) {
        if (messageId == null || !isDebugEnabledForCurrentUser()) {
            return Optional.empty();
        }
        return debugRepository.findByAiChatMessage_Id(messageId).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public boolean isDebugEnabledForCurrentUser() {
        return userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .map(User::isAiChatDebug)
                .orElse(false);
    }

    public AiChatMessageDebugResponse toResponse(AiChatMessageDebug entity) {
        return new AiChatMessageDebugResponse(
                entity.getId(),
                entity.getAiChatMessage().getId(),
                entity.getHandler(),
                entity.getToolName(),
                entity.getToolNames(),
                entity.getParams(),
                entity.isRagUsed(),
                entity.getAnswerSource() != null ? entity.getAnswerSource().name() : null,
                entity.getRouteReason(),
                entity.getFallbackReason(),
                entity.getErrorMessage(),
                entity.getCreatedAt()
        );
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
