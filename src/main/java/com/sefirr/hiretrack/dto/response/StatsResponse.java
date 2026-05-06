package com.sefirr.hiretrack.dto.response;

import com.sefirr.hiretrack.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class StatsResponse {

    @Data
    @Builder
    public static class Summary {
        private long totalApplications;
        private Map<ApplicationStatus, Long> byStatus;
        private double responseRate;
        private double interviewConversionRate;
        private double avgDaysToResponse;
    }

    @Data
    @Builder
    public static class TimelineEntry {
        private String week;
        private long count;
    }

    @Data
    @Builder
    public static class FunnelStage {
        private ApplicationStatus status;
        private long count;
        private double conversionFromPrevious;
    }
}
