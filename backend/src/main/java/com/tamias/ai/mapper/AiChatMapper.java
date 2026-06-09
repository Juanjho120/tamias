package com.tamias.ai.mapper;

import com.tamias.ai.dto.AiChatMessageResponse;
import com.tamias.ai.dto.AiChatSessionResponse;
import com.tamias.ai.dto.AiChatSessionSummaryResponse;
import com.tamias.ai.entity.AiChatMessage;
import com.tamias.ai.entity.AiChatSession;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AiChatMapper {

    public AiChatSessionSummaryResponse toSummaryResponse(AiChatSession entity, long messageCount) {
        var property = entity.getProperty();
        var createdBy = entity.getCreatedBy();

        return new AiChatSessionSummaryResponse(
                entity.getId(),
                property != null ? property.getId() : null,
                property != null ? property.getName() : null,
                entity.getTitle(),
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getFirstName() + " " + createdBy.getLastName() : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                messageCount
        );
    }

    public AiChatSessionResponse toResponse(AiChatSession entity, List<AiChatMessage> messages) {
        var property = entity.getProperty();
        var createdBy = entity.getCreatedBy();

        return new AiChatSessionResponse(
                entity.getId(),
                property != null ? property.getId() : null,
                property != null ? property.getName() : null,
                entity.getTitle(),
                createdBy != null ? createdBy.getId() : null,
                createdBy != null ? createdBy.getFirstName() + " " + createdBy.getLastName() : null,
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                messages.stream().map(this::toMessageResponse).toList()
        );
    }

    public AiChatMessageResponse toMessageResponse(AiChatMessage entity) {
        return new AiChatMessageResponse(
                entity.getId(),
                entity.getChatSession().getId(),
                entity.getRole(),
                entity.getContent(),
                entity.getCreatedAt()
        );
    }
}
