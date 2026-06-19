ALTER TABLE users
ADD COLUMN IF NOT EXISTS ai_chat_debug BOOLEAN NOT NULL DEFAULT false;

CREATE TABLE ai_chat_message_debugs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ai_chat_message_id UUID NOT NULL,
    handler VARCHAR(150),
    tool_name VARCHAR(150),
    tool_names JSONB NOT NULL DEFAULT '[]'::jsonb,
    params JSONB NOT NULL DEFAULT '{}'::jsonb,
    rag_used BOOLEAN NOT NULL DEFAULT false,
    answer_source VARCHAR(50) NOT NULL,
    route_reason VARCHAR(500),
    fallback_reason VARCHAR(500),
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_ai_chat_message_debugs_message
        FOREIGN KEY (ai_chat_message_id)
        REFERENCES ai_chat_messages(id)
        ON DELETE CASCADE,
    CONSTRAINT chk_ai_chat_message_debugs_answer_source
        CHECK (answer_source IN ('BACKEND_DIRECT', 'LLM_COMPOSED', 'RAG', 'TOOLS_AND_RAG', 'NO_MATCH', 'ERROR'))
);

CREATE UNIQUE INDEX ux_ai_chat_message_debugs_message_id
    ON ai_chat_message_debugs(ai_chat_message_id);

CREATE INDEX idx_ai_chat_message_debugs_tool_name
    ON ai_chat_message_debugs(tool_name);

CREATE INDEX idx_ai_chat_message_debugs_answer_source
    ON ai_chat_message_debugs(answer_source);

CREATE INDEX idx_ai_chat_message_debugs_created_at
    ON ai_chat_message_debugs(created_at);
