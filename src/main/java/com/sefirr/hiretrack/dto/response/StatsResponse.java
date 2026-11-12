package com.sefirr.hiretrack.dto.response;

import com.sefirr.hiretrack.enums.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

public final class StatsResponse {

    private StatsResponse() {
    }

    // No-args constructors are needed so Jackson can read these back from the Redis cache

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private long totalApplications;
        private Map<ApplicationStatus, Long> byStatus;
        private double responseRate;
        private double interviewConversionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineEntry {
        private String week;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FunnelStage {
        private ApplicationStatus status;
        private long count;
        private double conversionFromPrevious;
    }
}
