package com.sefirr.hiretrack.controller;

import com.sefirr.hiretrack.dto.response.DocumentResponse;
import com.sefirr.hiretrack.entity.User;
import com.sefirr.hiretrack.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Documents")
@SecurityRequirement(name = "Bearer Auth")
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping("/api/v1/applications/{appId}/documents")
    @Operation(summary = "List documents for an application")
    public List<DocumentResponse> getAll(@PathVariable UUID appId, @AuthenticationPrincipal User user) {
        return documentService.getByApplication(appId, user.getId());
    }

    @PostMapping(value = "/api/v1/applications/{appId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Upload a document")
    public DocumentResponse upload(
            @PathVariable UUID appId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user
    ) {
        return documentService.upload(appId, user.getId(), file);
    }

    @GetMapping("/api/v1/documents/{id}/download")
    @Operation(summary = "Download a document")
    public ResponseEntity<Resource> download(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        Resource resource = documentService.download(id, user.getId());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/api/v1/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a document")
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        documentService.delete(id, user.getId());
    }
}
