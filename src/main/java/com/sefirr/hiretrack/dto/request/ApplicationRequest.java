package com.sefirr.hiretrack.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDate;

@Data
public class ApplicationRequest {

    @NotBlank
    @Size(max = 255)
    private String company;

    @NotBlank
    @Size(max = 255)
    private String role;

    @NotNull
    private LocalDate appliedAt;

    @URL
    private String jobUrl;

    @Min(0)
    private Integer salaryMin;

    @Min(0)
    private Integer salaryMax;

    @Size(max = 255)
    private String location;

    private Boolean remote;
    private String notes;

    @JsonIgnore
    @AssertTrue(message = "salaryMin must be less than or equal to salaryMax")
    public boolean isSalaryRangeValid() {
        return salaryMin == null || salaryMax == null || salaryMin <= salaryMax;
    }
}
