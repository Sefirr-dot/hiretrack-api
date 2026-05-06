package com.sefirr.hiretrack.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Data
public class ApplicationRequest {

    @NotBlank
    private String company;

    @NotBlank
    private String role;

    @NotNull
    private LocalDate appliedAt;

    @URL
    private String jobUrl;

    @Min(0)
    private Integer salaryMin;

    @Min(0)
    private Integer salaryMax;

    private String location;
    private Boolean remote;
    private String notes;
}
