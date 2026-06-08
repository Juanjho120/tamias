package com.tamias.document.repository;

import com.tamias.document.entity.DocumentChunk;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    List<DocumentChunk> findByDocument_IdOrderByChunkIndexAsc(UUID documentId);

    void deleteByDocument_Id(UUID documentId);
}
