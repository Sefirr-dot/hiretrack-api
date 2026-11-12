package com.sefirr.hiretrack.dto.request;

import com.sefirr.hiretrack.enums.InterviewType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InterviewRequest {

    @NotNull
    private InterviewType type;

    // Not @Future: past interviews must stay editable (e.g. to add feedback afterwards)
    @NotNull
    private LocalDateTime scheduledAt;

    @Min(1)
    @Max(1440)
    private Integer durationMinutes;

    @Size(max = 100)
    private String platform;

    @Size(max = 255)
    private String interviewerName;

    private String notes;
    private String feedback;
}
