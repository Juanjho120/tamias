package com.tamias.ai.service;

import com.tamias.ai.dto.AiChatMessageDebugResponse;
import com.tamias.ai.dto.AiChatMessageResponse;
import com.tamias.ai.dto.AiChatSessionCreateRequest;
import com.tamias.ai.dto.AiChatSessionResponse;
import com.tamias.ai.dto.AiChatSessionSummaryResponse;
import com.tamias.ai.dto.AiChatSessionUpdateRequest;
import com.tamias.ai.entity.AiChatMessage;
import com.tamias.ai.entity.AiChatSession;
import com.tamias.ai.enums.AiChatMessageRole;
import com.tamias.ai.mapper.AiChatMapper;
import com.tamias.ai.repository.AiChatMessageDebugRepository;
import com.tamias.ai.repository.AiChatMessageRepository;
import com.tamias.ai.repository.AiChatSessionRepository;
import com.tamias.ai.tool.support.AiToolTextNormalizer;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.NotFoundException;
import com.tamias.organization.entity.Organization;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.property.entity.Property;
import com.tamias.property.repository.PropertyRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.entity.User;
import com.tamias.user.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiChatSessionService {

    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final AiChatMessageDebugRepository debugRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final AiChatMapper mapper;
    private final AiChatDebugTraceService debugTraceService;

    public AiChatSessionService(
            AiChatSessionRepository sessionRepository,
            AiChatMessageRepository messageRepository,
            AiChatMessageDebugRepository debugRepository,
            OrganizationRepository organizationRepository,
            PropertyRepository propertyRepository,
            UserRepository userRepository,
            CurrentUserService currentUserService,
            AiChatMapper mapper,
            AiChatDebugTraceService debugTraceService
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.debugRepository = debugRepository;
        this.organizationRepository = organizationRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.mapper = mapper;
        this.debugTraceService = debugTraceService;
    }

    @Transactional(readOnly = true)
    public PageResponse<AiChatSessionSummaryResponse> findAll(UUID propertyId, Pageable pageable) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        UUID currentUserId = currentUserService.getCurrentUserId();

        var page = propertyId == null
                ? sessionRepository.findByOrganization_IdAndCreatedBy_Id(organizationId, currentUserId, pageable)
                : sessionRepository.findByOrganization_IdAndCreatedBy_IdAndProperty_Id(
                        organizationId,
                        currentUserId,
                        propertyId,
                        pageable
                );

        return PageResponse.from(page.map(session -> mapper.toSummaryResponse(
                session,
                messageRepository.countByChatSession_IdAndOrganization_Id(session.getId(), organizationId)
        )));
    }

    @Transactional(readOnly = true)
    public AiChatSessionResponse findById(UUID sessionId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        AiChatSession session = findEntity(sessionId);
        List<AiChatMessage> messages = messageRepository
                .findByChatSession_IdAndOrganization_IdOrderByCreatedAtAsc(session.getId(), organizationId);

        return mapper.toResponse(session, messages, debugByMessageId(messages));
    }

    @Transactional(readOnly = true)
    public List<AiChatMessageResponse> findMessages(UUID sessionId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        AiChatSession session = findEntity(sessionId);

        return messageRepository
                .findByChatSession_IdAndOrganization_IdOrderByCreatedAtAsc(session.getId(), organizationId)
                .stream()
                .map(message -> mapper.toMessageResponse(
                        message,
                        debugTraceService.findDebugForMessageIfEnabled(message).orElse(null)
                ))
                .toList();
    }

    @Transactional
    public AiChatSessionResponse create(AiChatSessionCreateRequest request) {
        AiChatSession session = createSession(request.propertyId(), request.title());
        return mapper.toResponse(session, List.of());
    }

    @Transactional
    public AiChatSessionSummaryResponse updateTitle(UUID sessionId, AiChatSessionUpdateRequest request) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        AiChatSession session = findEntity(sessionId);
        session.setTitle(request.title());

        AiChatSession saved = sessionRepository.save(session);
        return mapper.toSummaryResponse(
                saved,
                messageRepository.countByChatSession_IdAndOrganization_Id(saved.getId(), organizationId)
        );
    }

    @Transactional
    public void delete(UUID sessionId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        AiChatSession session = findEntity(sessionId);

        debugRepository.deleteByChatSessionId(session.getId());
        messageRepository.deleteByChatSessionIdAndOrganizationId(session.getId(), organizationId);
        sessionRepository.delete(session);
    }

    @Transactional
    public AiChatSession getOrCreateSession(UUID sessionId, UUID propertyId, String title, String question) {
        if (sessionId != null) {
            return findEntity(sessionId);
        }

        String effectiveTitle = title != null && !title.isBlank() ? title.trim() : buildDefaultTitle(question);
        return createSession(propertyId, effectiveTitle);
    }

    @Transactional
    public AiChatMessage saveMessage(
            AiChatSession session,
            AiChatMessageRole role,
            String content
    ) {
        AiChatMessage message = new AiChatMessage();
        message.setOrganization(session.getOrganization());
        message.setChatSession(session);
        message.setRole(role);
        message.setContent(content);

        return messageRepository.save(message);
    }

    private AiChatSession createSession(UUID propertyId, String title) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Organization organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));
        User currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new NotFoundException("User not found"));
        Property property = propertyId == null
                ? null
                : propertyRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(propertyId, organizationId)
                        .orElseThrow(() -> new NotFoundException("Property not found"));

        AiChatSession session = new AiChatSession();
        session.setOrganization(organization);
        session.setProperty(property);
        session.setTitle(title);
        session.setCreatedBy(currentUser);

        return sessionRepository.save(session);
    }

    private AiChatSession findEntity(UUID sessionId) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        UUID currentUserId = currentUserService.getCurrentUserId();

        return sessionRepository.findByIdAndOrganization_IdAndCreatedBy_Id(sessionId, organizationId, currentUserId)
                .orElseThrow(() -> new NotFoundException("AI chat session not found"));
    }

    private Map<UUID, AiChatMessageDebugResponse> debugByMessageId(List<AiChatMessage> messages) {
        if (messages == null || messages.isEmpty() || !debugTraceService.isDebugEnabledForCurrentUser()) {
            return Map.of();
        }

        return messages.stream()
                .map(message -> debugTraceService.findDebugForMessageIfEnabled(message)
                        .map(debug -> Map.entry(message.getId(), debug)))
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String buildDefaultTitle(String question) {
        if (question == null || question.isBlank()) {
            return "New AI chat";
        }

        String normalized = collapseWhitespace(question.trim());
        if (normalized.length() <= 70) {
            return normalized;
        }

        return normalized.substring(0, 70).trim() + "...";
    }

    private String collapseWhitespace(String value) {
        return AiToolTextNormalizer.collapseWhitespace(value);
    }
}
