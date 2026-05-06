package com.sefirr.hiretrack.dto.request;

import com.sefirr.hiretrack.enums.InterviewType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewRequest {

    @NotNull
    private InterviewType type;

    @NotNull
    @Future
    private LocalDateTime scheduledAt;

    private Integer durationMinutes;
    private String platform;
    private String interviewerName;
    private String notes;
}
