package com.sefirr.hiretrack.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private UUID id;
    private UUID applicationId;
    private String originalFilename;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
}
