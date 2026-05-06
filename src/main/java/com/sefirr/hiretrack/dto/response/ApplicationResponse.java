package com.sefirr.hiretrack.dto.response;

import com.sefirr.hiretrack.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ApplicationResponse {
    private UUID id;
    private String company;
    private String role;
    private ApplicationStatus status;
    private String jobUrl;
    private Integer salaryMin;
    private Integer salaryMax;
    private String location;
    private Boolean remote;
    private String notes;
    private LocalDate appliedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
