package com.sefirr.hiretrack.dto.request;

import com.sefirr.hiretrack.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusUpdateRequest {

    @NotNull
    private ApplicationStatus status;
}
