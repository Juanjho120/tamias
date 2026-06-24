CREATE TABLE payment_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NULL,
    updated_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    deleted_by UUID NULL,
    CONSTRAINT fk_payment_categories_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_payment_categories_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_payment_categories_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT fk_payment_categories_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id),
    CONSTRAINT uk_payment_categories_organization_name UNIQUE (organization_id, name),
    CONSTRAINT chk_payment_categories_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED'))
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    property_id UUID NULL,
    category_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    method VARCHAR(30) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    responsible VARCHAR(150),
    pay_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NULL,
    updated_by UUID NULL,
    deleted_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP WITH TIME ZONE NULL,
    CONSTRAINT fk_payments_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_payments_property FOREIGN KEY (property_id) REFERENCES properties(id),
    CONSTRAINT fk_payments_category FOREIGN KEY (category_id) REFERENCES payment_categories(id),
    CONSTRAINT fk_payments_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_payments_updated_by FOREIGN KEY (updated_by) REFERENCES users(id),
    CONSTRAINT fk_payments_deleted_by FOREIGN KEY (deleted_by) REFERENCES users(id),
    CONSTRAINT chk_payments_method CHECK (method IN ('CREDIT', 'DEBIT', 'CASH', 'BANK_TRANSFER')),
    CONSTRAINT chk_payments_amount CHECK (amount >= 0),
    CONSTRAINT chk_payments_status CHECK (status IN ('ACTIVE', 'DELETED'))
);

CREATE TABLE payment_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    s3_key TEXT NOT NULL,
    filepath VARCHAR(300),
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_by UUID NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_payment_images_organization FOREIGN KEY (organization_id) REFERENCES organizations(id),
    CONSTRAINT fk_payment_images_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    CONSTRAINT fk_payment_images_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT chk_payment_images_status CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_payment_images_size_bytes CHECK (size_bytes >= 0)
);

CREATE INDEX idx_payment_categories_organization_status ON payment_categories(organization_id, status);
CREATE INDEX idx_payments_org_pay_date ON payments(organization_id, pay_date);
CREATE INDEX idx_payments_org_status ON payments(organization_id, status);
CREATE INDEX idx_payments_property ON payments(property_id);
CREATE INDEX idx_payments_category ON payments(category_id);
CREATE INDEX idx_payments_method ON payments(method);
CREATE INDEX idx_payment_images_payment ON payment_images(payment_id);
CREATE INDEX idx_payment_images_organization ON payment_images(organization_id);
CREATE INDEX idx_payment_images_org_status ON payment_images(organization_id, status);
CREATE INDEX idx_payment_images_org_payment_status ON payment_images(organization_id, payment_id, status);

CREATE TRIGGER trg_payment_categories_set_updated_at
BEFORE UPDATE ON payment_categories
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TRIGGER trg_payments_set_updated_at
BEFORE UPDATE ON payments
FOR EACH ROW EXECUTE FUNCTION set_updated_at();
