CREATE TABLE ai_chat_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    property_id UUID NULL,
    title VARCHAR(150),
    created_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ai_chat_sessions_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_ai_chat_sessions_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id),

    CONSTRAINT fk_ai_chat_sessions_created_by
        FOREIGN KEY (created_by)
        REFERENCES users(id)
);

CREATE TABLE ai_chat_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    chat_session_id UUID NOT NULL,
    role VARCHAR(30) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_ai_chat_messages_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_ai_chat_messages_session
        FOREIGN KEY (chat_session_id)
        REFERENCES ai_chat_sessions(id),

    CONSTRAINT chk_ai_chat_messages_role
        CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL'))
);

CREATE INDEX idx_ai_chat_sessions_org
    ON ai_chat_sessions(organization_id);

CREATE INDEX idx_ai_chat_sessions_property
    ON ai_chat_sessions(property_id);

CREATE INDEX idx_ai_chat_sessions_created_at
    ON ai_chat_sessions(organization_id, created_at DESC);

CREATE INDEX idx_ai_chat_messages_session
    ON ai_chat_messages(chat_session_id);

CREATE INDEX idx_ai_chat_messages_org
    ON ai_chat_messages(organization_id);

CREATE INDEX idx_ai_chat_messages_created_at
    ON ai_chat_messages(chat_session_id, created_at);

CREATE TRIGGER trg_ai_chat_sessions_set_updated_at
BEFORE UPDATE ON ai_chat_sessions
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
