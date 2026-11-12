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
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final int MAX_FILENAME_LENGTH = 255;

    private final DocumentRepository documentRepository;
    private final ApplicationRepository applicationRepository;
    private final StorageService storageService;

    public record DocumentDownload(Resource resource, String filename, String contentType) {}

    @Transactional(readOnly = true)
    public List<DocumentResponse> getByApplication(UUID applicationId, UUID userId) {
        verifyOwnership(applicationId, userId);
        return documentRepository.findByApplicationId(applicationId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public DocumentResponse upload(UUID applicationId, UUID userId, MultipartFile file) {
        Application app = verifyOwnership(applicationId, userId);
        String storagePath = storageService.store(file, applicationId.toString());

        try {
            Document doc = Document.builder()
                    .application(app)
                    .filename(storagePath.substring(storagePath.lastIndexOf('/') + 1))
                    .originalFilename(sanitizeFilename(file.getOriginalFilename()))
                    .contentType(file.getContentType())
                    .sizeBytes(file.getSize())
                    .storagePath(storagePath)
                    .build();
            return toResponse(documentRepository.saveAndFlush(doc));
        } catch (RuntimeException e) {
            // Don't leave an orphan file on disk if the DB insert fails
            storageService.delete(storagePath);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public DocumentDownload download(UUID documentId, UUID userId) {
        Document doc = findOwned(documentId, userId);
        return new DocumentDownload(
                storageService.load(doc.getStoragePath()),
                doc.getOriginalFilename(),
                doc.getContentType()
        );
    }

    @Transactional
    public void delete(UUID documentId, UUID userId) {
        Document doc = findOwned(documentId, userId);
        documentRepository.delete(doc);
        documentRepository.flush();
        // Only remove the file once the row is gone, so a DB failure can't leave a dangling row
        storageService.delete(doc.getStoragePath());
    }

    private Document findOwned(UUID documentId, UUID userId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        verifyOwnership(doc.getApplication().getId(), userId);
        return doc;
    }

    private Application verifyOwnership(UUID applicationId, UUID userId) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
        if (!app.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied");
        }
        return app;
    }

    /** Strips any client-side path and caps the length to fit the column. */
    private static String sanitizeFilename(String original) {
        String name = StringUtils.getFilename(StringUtils.cleanPath(original == null ? "" : original.replace('\\', '/')));
        if (!StringUtils.hasText(name)) {
            name = "document";
        }
        if (name.length() > MAX_FILENAME_LENGTH) {
            int dot = name.lastIndexOf('.');
            String ext = dot > 0 && name.length() - dot <= 10 ? name.substring(dot) : "";
            name = name.substring(0, MAX_FILENAME_LENGTH - ext.length()) + ext;
        }
        return name;
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
