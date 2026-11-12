package com.sefirr.hiretrack.dto.response;

import com.sefirr.hiretrack.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
