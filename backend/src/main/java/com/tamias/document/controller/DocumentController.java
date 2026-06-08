package com.tamias.document.controller;

import com.tamias.common.dto.PageResponse;
import com.tamias.document.dto.DocumentChunkResponse;
import com.tamias.document.dto.DocumentDownloadUrlResponse;
import com.tamias.document.dto.DocumentIndexingResponse;
import com.tamias.document.dto.DocumentProcessingResponse;
import com.tamias.document.dto.DocumentResponse;
import com.tamias.document.dto.DocumentSummaryResponse;
import com.tamias.document.dto.DocumentUploadRequest;
import com.tamias.document.enums.DocumentProcessingStatus;
import com.tamias.document.enums.DocumentStatus;
import com.tamias.document.enums.DocumentType;
import com.tamias.document.service.DocumentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public PageResponse<DocumentSummaryResponse> findAll(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam(required = false) DocumentType documentType,
            @RequestParam(required = false) DocumentProcessingStatus processingStatus,
            @RequestParam(required = false) DocumentStatus status,
            Pageable pageable
    ) {
        return documentService.findAll(propertyId, documentType, processingStatus, status, pageable);
    }

    @GetMapping("/{id}")
    public DocumentResponse findById(@PathVariable UUID id) {
        return documentService.findById(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(
            @RequestParam(required = false) UUID propertyId,
            @RequestParam DocumentType documentType,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestPart("file") MultipartFile file
    ) {
        DocumentUploadRequest request = new DocumentUploadRequest(
                propertyId,
                documentType,
                title,
                description
        );

        return documentService.upload(request, file);
    }

    @GetMapping("/{id}/download-url")
    public DocumentDownloadUrlResponse getDownloadUrl(@PathVariable UUID id) {
        return documentService.getDownloadUrl(id);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> getFile(@PathVariable UUID id) {
        Resource resource = documentService.getFile(id);

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(resource.getFilename() != null ? resource.getFilename() : "document")
                                .build()
                                .toString()
                )
                .body(resource);
    }

    @PostMapping("/{id}/process")
    public DocumentProcessingResponse process(@PathVariable UUID id) {
        return documentService.process(id);
    }

    @PostMapping("/{id}/index")
    public DocumentIndexingResponse index(@PathVariable UUID id) {
        return documentService.index(id);
    }

    @GetMapping("/{id}/chunks")
    public List<DocumentChunkResponse> findChunks(@PathVariable UUID id) {
        return documentService.findChunks(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        documentService.delete(id);
    }
}
