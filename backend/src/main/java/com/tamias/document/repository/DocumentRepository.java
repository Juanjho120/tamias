package com.tamias.document.repository;

import com.tamias.document.entity.Document;
import com.tamias.document.enums.DocumentProcessingStatus;
import com.tamias.document.enums.DocumentStatus;
import com.tamias.document.enums.DocumentType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findByIdAndOrganization_IdAndDeletedAtIsNull(UUID id, UUID organizationId);

    Page<Document> findByOrganization_IdAndDeletedAtIsNull(UUID organizationId, Pageable pageable);

    Page<Document> findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(
            UUID organizationId,
            UUID propertyId,
            Pageable pageable
    );

    Page<Document> findByOrganization_IdAndDocumentTypeAndDeletedAtIsNull(
            UUID organizationId,
            DocumentType documentType,
            Pageable pageable
    );

    Page<Document> findByOrganization_IdAndProcessingStatusAndDeletedAtIsNull(
            UUID organizationId,
            DocumentProcessingStatus processingStatus,
            Pageable pageable
    );

    Page<Document> findByOrganization_IdAndStatusAndDeletedAtIsNull(
            UUID organizationId,
            DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findByOrganization_IdAndProperty_IdAndDocumentTypeAndDeletedAtIsNull(
            UUID organizationId,
            UUID propertyId,
            DocumentType documentType,
            Pageable pageable
    );
}
