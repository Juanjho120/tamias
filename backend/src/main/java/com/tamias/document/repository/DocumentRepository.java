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

    Optional<Document> findByIdAndOrganization_Id(UUID id, UUID organizationId);

    Page<Document> findByOrganization_Id(UUID organizationId, Pageable pageable);

    Page<Document> findByOrganization_IdAndProperty_Id(
            UUID organizationId,
            UUID propertyId,
            Pageable pageable
    );

    Page<Document> findByOrganization_IdAndDocumentType(
            UUID organizationId,
            DocumentType documentType,
            Pageable pageable
    );

    Page<Document> findByOrganization_IdAndProcessingStatus(
            UUID organizationId,
            DocumentProcessingStatus processingStatus,
            Pageable pageable
    );

    Page<Document> findByOrganization_IdAndStatus(
            UUID organizationId,
            DocumentStatus status,
            Pageable pageable
    );

    Page<Document> findByOrganization_IdAndProperty_IdAndDocumentType(
            UUID organizationId,
            UUID propertyId,
            DocumentType documentType,
            Pageable pageable
    );
}
