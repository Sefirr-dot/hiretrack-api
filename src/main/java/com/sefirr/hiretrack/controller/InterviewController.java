package com.sefirr.hiretrack.controller;

import com.sefirr.hiretrack.dto.request.InterviewRequest;
import com.sefirr.hiretrack.dto.response.InterviewResponse;
import com.sefirr.hiretrack.entity.User;
import com.sefirr.hiretrack.service.InterviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Interviews")
@SecurityRequirement(name = "Bearer Auth")
public class InterviewController {

    private final InterviewService interviewService;

    @GetMapping("/api/v1/applications/{appId}/interviews")
    @Operation(summary = "List interviews for an application")
    public List<InterviewResponse> getAll(@PathVariable UUID appId, @AuthenticationPrincipal User user) {
        return interviewService.getByApplication(appId, user.getId());
    }

    @PostMapping("/api/v1/applications/{appId}/interviews")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Schedule an interview")
    public InterviewResponse create(
            @PathVariable UUID appId,
            @Valid @RequestBody InterviewRequest request,
            @AuthenticationPrincipal User user
    ) {
        return interviewService.create(appId, user.getId(), request);
    }

    @PutMapping("/api/v1/interviews/{id}")
    @Operation(summary = "Update an interview")
    public InterviewResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody InterviewRequest request,
            @AuthenticationPrincipal User user
    ) {
        return interviewService.update(id, user.getId(), request);
    }

    @DeleteMapping("/api/v1/interviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an interview")
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        interviewService.delete(id, user.getId());
    }
}
