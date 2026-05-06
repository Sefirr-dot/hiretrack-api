package com.sefirr.hiretrack.dto.response;

import com.sefirr.hiretrack.enums.InterviewType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class InterviewResponse {
    private UUID id;
    private UUID applicationId;
    private InterviewType type;
    private LocalDateTime scheduledAt;
    private Integer durationMinutes;
    private String platform;
    private String interviewerName;
    private String notes;
    private String feedback;
    private LocalDateTime createdAt;
}
