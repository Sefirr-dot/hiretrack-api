package com.sefirr.hiretrack.repository;

import com.sefirr.hiretrack.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByApplicationId(UUID applicationId);
}
