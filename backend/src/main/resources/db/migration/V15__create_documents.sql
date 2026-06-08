CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    property_id UUID NULL,
    document_type VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    original_filename VARCHAR(255) NOT NULL,
    s3_key TEXT NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    processing_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    uploaded_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,

    CONSTRAINT fk_documents_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_documents_property
        FOREIGN KEY (property_id)
        REFERENCES properties(id),

    CONSTRAINT fk_documents_uploaded_by
        FOREIGN KEY (uploaded_by)
        REFERENCES users(id),

    CONSTRAINT fk_documents_deleted_by
        FOREIGN KEY (deleted_by)
        REFERENCES users(id),

    CONSTRAINT chk_documents_type
        CHECK (document_type IN (
            'HOUSE_RULES',
            'BATHROOM_RULES',
            'PROPERTY_SIGNS',
            'BLUEPRINT',
            'ELECTRICAL_PLAN',
            'PLUMBING_PLAN',
            'DRAINAGE_PLAN',
            'MANUAL',
            'OTHER'
        )),

    CONSTRAINT chk_documents_processing_status
        CHECK (processing_status IN ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED')),

    CONSTRAINT chk_documents_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),

    CONSTRAINT chk_documents_size_bytes
        CHECK (size_bytes >= 0)
);

CREATE TABLE document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_count INTEGER NULL,
    vector_store_collection VARCHAR(150),
    vector_store_id VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_document_chunks_organization
        FOREIGN KEY (organization_id)
        REFERENCES organizations(id),

    CONSTRAINT fk_document_chunks_document
        FOREIGN KEY (document_id)
        REFERENCES documents(id),

    CONSTRAINT uk_document_chunks_document_index
        UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_documents_org_property
    ON documents(organization_id, property_id);

CREATE INDEX idx_documents_org_type
    ON documents(organization_id, document_type);

CREATE INDEX idx_documents_processing_status
    ON documents(processing_status);

CREATE INDEX idx_documents_org_status
    ON documents(organization_id, status);

CREATE INDEX idx_document_chunks_document
    ON document_chunks(document_id);

CREATE INDEX idx_document_chunks_org
    ON document_chunks(organization_id);

CREATE INDEX idx_document_chunks_vector_store_id
    ON document_chunks(vector_store_id);

CREATE TRIGGER trg_documents_set_updated_at
BEFORE UPDATE ON documents
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
