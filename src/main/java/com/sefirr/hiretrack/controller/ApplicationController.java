package com.sefirr.hiretrack.controller;

import com.sefirr.hiretrack.dto.request.ApplicationRequest;
import com.sefirr.hiretrack.dto.request.StatusUpdateRequest;
import com.sefirr.hiretrack.dto.response.ApplicationResponse;
import com.sefirr.hiretrack.dto.response.PageResponse;
import com.sefirr.hiretrack.entity.User;
import com.sefirr.hiretrack.enums.ApplicationStatus;
import com.sefirr.hiretrack.service.ApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
@Tag(name = "Applications")
@SecurityRequirement(name = "Bearer Auth")
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping
    @Operation(summary = "List all applications with optional filters")
    public PageResponse<ApplicationResponse> getAll(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) Boolean remote,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return applicationService.getAll(
                user.getId(),
                PageRequest.of(page, size, Sort.by("appliedAt").descending()),
                status, company, remote, from, to
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application by ID")
    public ApplicationResponse getById(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        return applicationService.getById(id, user.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new application")
    public ApplicationResponse create(
            @Valid @RequestBody ApplicationRequest request,
            @AuthenticationPrincipal User user
    ) {
        return applicationService.create(user.getId(), request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an application")
    public ApplicationResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ApplicationRequest request,
            @AuthenticationPrincipal User user
    ) {
        return applicationService.update(id, user.getId(), request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update application status")
    public ApplicationResponse updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request,
            @AuthenticationPrincipal User user
    ) {
        return applicationService.updateStatus(id, user.getId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an application")
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        applicationService.delete(id, user.getId());
    }
}
