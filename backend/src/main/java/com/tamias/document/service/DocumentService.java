package com.tamias.document.service;

import com.tamias.ai.service.RagVectorStoreService;
import com.tamias.common.dto.PageResponse;
import com.tamias.common.exception.BadRequestException;
import com.tamias.common.exception.NotFoundException;
import com.tamias.document.dto.DocumentChunkResponse;
import com.tamias.document.dto.DocumentDownloadUrlResponse;
import com.tamias.document.dto.DocumentIndexingResponse;
import com.tamias.document.dto.DocumentProcessingResponse;
import com.tamias.document.dto.DocumentResponse;
import com.tamias.document.dto.DocumentSummaryResponse;
import com.tamias.document.dto.DocumentUploadRequest;
import com.tamias.document.entity.Document;
import com.tamias.document.enums.DocumentProcessingStatus;
import com.tamias.document.enums.DocumentStatus;
import com.tamias.document.enums.DocumentType;
import com.tamias.document.mapper.DocumentMapper;
import com.tamias.document.processing.DocumentProcessingService;
import com.tamias.document.repository.DocumentChunkRepository;
import com.tamias.document.repository.DocumentRepository;
import com.tamias.document.storage.FileStorageService;
import com.tamias.organization.repository.OrganizationRepository;
import com.tamias.property.entity.Property;
import com.tamias.property.repository.PropertyRepository;
import com.tamias.security.service.CurrentUserService;
import com.tamias.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024L * 1024L;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
        "application/pdf",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain",
        "image/jpeg",
        "image/png",
        "image/webp"
    );

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final OrganizationRepository organizationRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final FileStorageService fileStorageService;
    private final DocumentProcessingService documentProcessingService;
    private final RagVectorStoreService ragVectorStoreService;
    private final DocumentMapper documentMapper;

    public DocumentService(
        DocumentRepository documentRepository,
        DocumentChunkRepository documentChunkRepository,
        OrganizationRepository organizationRepository,
        PropertyRepository propertyRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        FileStorageService fileStorageService,
        DocumentProcessingService documentProcessingService,
        RagVectorStoreService ragVectorStoreService,
        DocumentMapper documentMapper
    ) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.organizationRepository = organizationRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.fileStorageService = fileStorageService;
        this.documentProcessingService = documentProcessingService;
        this.ragVectorStoreService = ragVectorStoreService;
        this.documentMapper = documentMapper;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public PageResponse<DocumentSummaryResponse> findAll(
        UUID propertyId,
        DocumentType documentType,
        DocumentProcessingStatus processingStatus,
        DocumentStatus status,
        Pageable pageable
    ) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        Page<Document> page;

        if (propertyId != null && documentType != null) {
            page = documentRepository.findByOrganization_IdAndProperty_IdAndDocumentTypeAndDeletedAtIsNull(
                organizationId,
                propertyId,
                documentType,
                pageable
            );
        } else if (propertyId != null) {
            page = documentRepository.findByOrganization_IdAndProperty_IdAndDeletedAtIsNull(
                organizationId,
                propertyId,
                pageable
            );
        } else if (documentType != null) {
            page = documentRepository.findByOrganization_IdAndDocumentTypeAndDeletedAtIsNull(
                organizationId,
                documentType,
                pageable
            );
        } else if (processingStatus != null) {
            page = documentRepository.findByOrganization_IdAndProcessingStatusAndDeletedAtIsNull(
                organizationId,
                processingStatus,
                pageable
            );
        } else if (status != null) {
            page = documentRepository.findByOrganization_IdAndStatusAndDeletedAtIsNull(
                organizationId,
                status,
                pageable
            );
        } else {
            page = documentRepository.findByOrganization_IdAndDeletedAtIsNull(organizationId, pageable);
        }

        return PageResponse.from(page.map(documentMapper::toSummaryResponse));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public DocumentResponse findById(UUID id) {
        return documentMapper.toResponse(findDocument(id));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public DocumentResponse upload(DocumentUploadRequest request, MultipartFile file) {
        validateFile(file);

        UUID organizationId = currentUserService.getCurrentOrganizationId();
        var organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
            .orElseThrow(() -> new NotFoundException("Organization not found"));
        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        Property property = request.propertyId() == null
            ? null
            : propertyRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(request.propertyId(), organizationId)
                .orElseThrow(() -> new NotFoundException("Property not found"));

        String storageFolder = buildDocumentStorageFolder(property);
        var storedFile = fileStorageService.store(file, storageFolder);

        Document document = new Document();
        document.setOrganization(organization);
        document.setProperty(property);
        document.setDocumentType(request.documentType());
        document.setTitle(request.title());
        document.setDescription(request.description());
        document.setOriginalFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");
        document.setS3Key(storedFile.storageKey());
        document.setFilepath(storedFile.filepath());
        document.setContentType(storedFile.contentType());
        document.setSizeBytes(storedFile.sizeBytes());
        document.setProcessingStatus(DocumentProcessingStatus.PENDING);
        document.setStatus(DocumentStatus.ACTIVE);
        document.setUploadedBy(currentUser);

        return documentMapper.toResponse(documentRepository.save(document));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public DocumentDownloadUrlResponse getDownloadUrl(UUID id) {
        Document document = findDocument(id);
        return new DocumentDownloadUrlResponse(
            fileStorageService.buildDownloadUrl(document.getS3Key(), document.getId().toString()),
            fileStorageService.getDownloadUrlExpirationSeconds()
        );
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public Resource getFile(UUID id) {
        Document document = findDocument(id);
        return fileStorageService.loadAsResource(document.getS3Key());
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public DocumentProcessingResponse process(UUID id) {
        Document document = findDocument(id);
        return documentProcessingService.process(document);
    }

    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public DocumentIndexingResponse index(UUID id) {
        Document document = findDocument(id);
        return ragVectorStoreService.indexDocument(document);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER', 'MAINTENANCE_STAFF', 'READ_ONLY')")
    public List<DocumentChunkResponse> findChunks(UUID documentId) {
        Document document = findDocument(documentId);
        return documentChunkRepository.findByDocument_IdOrderByChunkIndexAsc(document.getId())
            .stream()
            .map(documentMapper::toChunkResponse)
            .toList();
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'PROPERTY_MANAGER')")
    public void delete(UUID id) {
        Document document = findDocument(id);
        ragVectorStoreService.deleteDocumentVectors(document);

        var currentUser = userRepository.findByIdAndDeletedAtIsNull(currentUserService.getCurrentUserId())
            .orElseThrow(() -> new NotFoundException("User not found"));

        document.setStatus(DocumentStatus.DELETED);
        document.setDeletedAt(OffsetDateTime.now());
        document.setDeletedBy(currentUser);
        documentRepository.save(document);
    }

    private Document findDocument(UUID id) {
        UUID organizationId = currentUserService.getCurrentOrganizationId();
        return documentRepository.findByIdAndOrganization_IdAndDeletedAtIsNull(id, organizationId)
            .orElseThrow(() -> new NotFoundException("Document not found"));
    }

    private String buildDocumentStorageFolder(Property property) {
        if (property == null) {
            return "documents";
        }
        return "documents/" + property.getId();
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File exceeds maximum allowed size");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("File type is not allowed");
        }
    }
}
