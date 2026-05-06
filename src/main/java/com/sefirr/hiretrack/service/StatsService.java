package com.sefirr.hiretrack.service;

import com.sefirr.hiretrack.dto.response.StatsResponse;
import com.sefirr.hiretrack.entity.Application;
import com.sefirr.hiretrack.enums.ApplicationStatus;
import com.sefirr.hiretrack.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ApplicationRepository applicationRepository;

    private static final DateTimeFormatter WEEK_FMT = DateTimeFormatter.ofPattern("yyyy-'W'ww");

    @Cacheable(value = "stats-summary", key = "#userId")
    public StatsResponse.Summary getSummary(UUID userId) {
        long total = applicationRepository.countByUserId(userId);

        Map<ApplicationStatus, Long> byStatus = Arrays.stream(ApplicationStatus.values())
                .collect(Collectors.toMap(
                        s -> s,
                        s -> applicationRepository.countByUserIdAndStatus(userId, s),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        long responded = byStatus.entrySet().stream()
                .filter(e -> e.getKey() != ApplicationStatus.APPLIED && e.getKey() != ApplicationStatus.GHOSTED)
                .mapToLong(Map.Entry::getValue).sum();

        long interviewed = byStatus.getOrDefault(ApplicationStatus.INTERVIEW, 0L)
                + byStatus.getOrDefault(ApplicationStatus.FINAL_INTERVIEW, 0L)
                + byStatus.getOrDefault(ApplicationStatus.OFFER, 0L)
                + byStatus.getOrDefault(ApplicationStatus.HIRED, 0L);

        double responseRate = total > 0 ? (double) responded / total : 0;
        double interviewRate = responded > 0 ? (double) interviewed / responded : 0;

        return StatsResponse.Summary.builder()
                .totalApplications(total)
                .byStatus(byStatus)
                .responseRate(Math.round(responseRate * 100.0) / 100.0)
                .interviewConversionRate(Math.round(interviewRate * 100.0) / 100.0)
                .avgDaysToResponse(0)
                .build();
    }

    @Cacheable(value = "stats-timeline", key = "#userId")
    public List<StatsResponse.TimelineEntry> getTimeline(UUID userId) {
        LocalDate from = LocalDate.now().minusWeeks(12);
        List<Application> apps = applicationRepository.findByUserIdAndAppliedAtAfter(userId, from);

        Map<String, Long> grouped = apps.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getAppliedAt().format(WEEK_FMT),
                        Collectors.counting()
                ));

        return grouped.entrySet().stream()
                .map(e -> StatsResponse.TimelineEntry.builder()
                        .week(e.getKey())
                        .count(e.getValue())
                        .build())
                .sorted((a, b) -> a.getWeek().compareTo(b.getWeek()))
                .toList();
    }

    @Cacheable(value = "stats-funnel", key = "#userId")
    public List<StatsResponse.FunnelStage> getFunnel(UUID userId) {
        ApplicationStatus[] stages = {
                ApplicationStatus.APPLIED,
                ApplicationStatus.PHONE_SCREEN,
                ApplicationStatus.TECHNICAL_TEST,
                ApplicationStatus.INTERVIEW,
                ApplicationStatus.FINAL_INTERVIEW,
                ApplicationStatus.OFFER,
                ApplicationStatus.HIRED
        };

        List<StatsResponse.FunnelStage> funnel = new java.util.ArrayList<>();
        long previousCount = 0;

        for (ApplicationStatus stage : stages) {
            long count = applicationRepository.countByUserIdAndStatus(userId, stage);
            double conversion = previousCount > 0 ? (double) count / previousCount : 0;
            funnel.add(StatsResponse.FunnelStage.builder()
                    .status(stage)
                    .count(count)
                    .conversionFromPrevious(stage == ApplicationStatus.APPLIED ? 1.0
                            : Math.round(conversion * 100.0) / 100.0)
                    .build());
            if (count > 0) previousCount = count;
        }

        return funnel;
    }
}
