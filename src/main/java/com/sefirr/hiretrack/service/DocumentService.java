package com.sefirr.hiretrack.service;

import com.sefirr.hiretrack.dto.response.DocumentResponse;
import com.sefirr.hiretrack.entity.Application;
import com.sefirr.hiretrack.entity.Document;
import com.sefirr.hiretrack.exception.ResourceNotFoundException;
import com.sefirr.hiretrack.exception.UnauthorizedException;
import com.sefirr.hiretrack.repository.ApplicationRepository;
import com.sefirr.hiretrack.repository.DocumentRepository;
import com.sefirr.hiretrack.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final StorageService storageService;

    public List<DocumentResponse> getByApplication(UUID applicationId, UUID userId) {
        verifyOwnership(applicationId, userId);
        return documentRepository.findByApplicationId(applicationId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DocumentResponse upload(UUID applicationId, UUID userId, MultipartFile file) {
        Application app = verifyOwnership(applicationId, userId);
        String storagePath = storageService.store(file, applicationId.toString());

        Document doc = Document.builder()
                .application(app)
                .filename(storagePath.substring(storagePath.lastIndexOf('/') + 1))
                .originalFilename(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .storagePath(storagePath)
                .build();

        return toResponse(documentRepository.save(doc));
    }

    public Resource download(UUID documentId, UUID userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        verifyOwnership(doc.getApplication().getId(), userId);
        return storageService.load(doc.getStoragePath());
    }

    @Transactional
    public void delete(UUID documentId, UUID userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        verifyOwnership(doc.getApplication().getId(), userId);
        storageService.delete(doc.getStoragePath());
        documentRepository.delete(doc);
    }

    private Application verifyOwnership(UUID applicationId, UUID userId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        if (!app.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return app;
    }

    private DocumentResponse toResponse(Document d) {
        return DocumentResponse.builder()
                .id(d.getId())
                .applicationId(d.getApplication().getId())
                .originalFilename(d.getOriginalFilename())
                .contentType(d.getContentType())
                .sizeBytes(d.getSizeBytes())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
