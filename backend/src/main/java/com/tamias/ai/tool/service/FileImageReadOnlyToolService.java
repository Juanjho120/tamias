package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.repository.FileImageToolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FileImageReadOnlyToolService {

    private final FileImageToolRepository repository;

    public FileImageReadOnlyToolService(FileImageToolRepository repository) {
        this.repository = repository;
    }

    public AiToolAnswer fileMetadata(String userQuestion) {
        return repository.fileMetadata(userQuestion);
    }

    public AiToolAnswer filesByProperty(String userQuestion) {
        return repository.filesByProperty(userQuestion);
    }

    public AiToolAnswer filesByMaintenance(String userQuestion) {
        return repository.filesByMaintenance(userQuestion);
    }

    public AiToolAnswer filesByDocument(String userQuestion) {
        return repository.filesByDocument(userQuestion);
    }

    public AiToolAnswer fileStorageSummary() {
        return repository.fileStorageSummary();
    }

    public AiToolAnswer orphanFileCandidates() {
        return repository.orphanFileCandidates();
    }

    public AiToolAnswer propertyImageMetadataSummary() {
        return repository.propertyImageMetadataSummary();
    }

    public AiToolAnswer maintenanceImageMetadataSummary() {
        return repository.maintenanceImageMetadataSummary();
    }

    public AiToolAnswer imageDashboardSummary() {
        return repository.imageDashboardSummary();
    }

    public AiToolAnswer recentUploads() {
        return repository.recentUploads();
    }

    public AiToolAnswer largestFiles() {
        return repository.largestFiles();
    }

    public AiToolAnswer entitiesWithoutImages() {
        return repository.entitiesWithoutImages();
    }

    public AiToolAnswer entitiesWithMostImages() {
        return repository.entitiesWithMostImages();
    }
}
