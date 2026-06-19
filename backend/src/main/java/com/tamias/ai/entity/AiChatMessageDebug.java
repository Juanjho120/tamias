package com.tamias.ai.entity;

import com.tamias.ai.enums.AiAnswerSource;
import com.tamias.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "ai_chat_message_debugs")
public class AiChatMessageDebug extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ai_chat_message_id", nullable = false)
    private AiChatMessage aiChatMessage;

    @Column(length = 150)
    private String handler;

    @Column(name = "tool_name", length = 150)
    private String toolName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tool_names", nullable = false, columnDefinition = "jsonb")
    private List<String> toolNames = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> params = new LinkedHashMap<>();

    @Column(name = "rag_used", nullable = false)
    private boolean ragUsed = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_source", nullable = false, length = 50)
    private AiAnswerSource answerSource = AiAnswerSource.NO_MATCH;

    @Column(name = "route_reason", length = 500)
    private String routeReason;

    @Column(name = "fallback_reason", length = 500)
    private String fallbackReason;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
