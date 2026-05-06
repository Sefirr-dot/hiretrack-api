package com.sefirr.hiretrack.controller;

import com.sefirr.hiretrack.dto.response.StatsResponse;
import com.sefirr.hiretrack.entity.User;
import com.sefirr.hiretrack.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Tag(name = "Statistics")
@SecurityRequirement(name = "Bearer Auth")
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/summary")
    @Operation(summary = "Get job search summary statistics")
    public StatsResponse.Summary getSummary(@AuthenticationPrincipal User user) {
        return statsService.getSummary(user.getId());
    }

    @GetMapping("/timeline")
    @Operation(summary = "Get weekly application timeline (last 12 weeks)")
    public List<StatsResponse.TimelineEntry> getTimeline(@AuthenticationPrincipal User user) {
        return statsService.getTimeline(user.getId());
    }

    @GetMapping("/funnel")
    @Operation(summary = "Get application funnel by stage")
    public List<StatsResponse.FunnelStage> getFunnel(@AuthenticationPrincipal User user) {
        return statsService.getFunnel(user.getId());
    }
}
