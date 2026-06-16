package com.tamias.ai.tool.service;

import com.tamias.ai.tool.AiToolAnswer;
import com.tamias.ai.tool.support.AiReadOnlyToolSupport;
import com.tamias.security.service.CurrentUserService;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FileImageReadOnlyToolService extends AiReadOnlyToolSupport {

    public FileImageReadOnlyToolService(EntityManager entityManager, CurrentUserService currentUserService) {
        super(entityManager, currentUserService);
    }

    public AiToolAnswer fileMetadata(String userQuestion) {
        return super.fileMetadata(userQuestion);
    }

    public AiToolAnswer filesByProperty(String userQuestion) {
        return super.filesByProperty(userQuestion);
    }

    public AiToolAnswer filesByMaintenance(String userQuestion) {
        return super.filesByMaintenance(userQuestion);
    }

    public AiToolAnswer filesByDocument(String userQuestion) {
        return super.filesByDocument(userQuestion);
    }

    public AiToolAnswer fileStorageSummary() {
        return super.fileStorageSummary();
    }

    public AiToolAnswer orphanFileCandidates() {
        return super.orphanFileCandidates();
    }

    public AiToolAnswer propertyImageMetadataSummary() {
        return super.propertyImageMetadataSummary();
    }

    public AiToolAnswer maintenanceImageMetadataSummary() {
        return super.maintenanceImageMetadataSummary();
    }

}
