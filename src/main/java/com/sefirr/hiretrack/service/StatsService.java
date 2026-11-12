package com.sefirr.hiretrack.service;

import com.sefirr.hiretrack.config.CacheNames;
import com.sefirr.hiretrack.dto.response.StatsResponse;
import com.sefirr.hiretrack.entity.Application;
import com.sefirr.hiretrack.enums.ApplicationStatus;
import com.sefirr.hiretrack.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    static final int TIMELINE_WEEKS = 12;

    /** Linear pipeline, in order. Terminal "negative" statuses are not part of it. */
    static final List<ApplicationStatus> PIPELINE = List.of(
            ApplicationStatus.APPLIED,
            ApplicationStatus.PHONE_SCREEN,
            ApplicationStatus.TECHNICAL_TEST,
            ApplicationStatus.INTERVIEW,
            ApplicationStatus.FINAL_INTERVIEW,
            ApplicationStatus.OFFER,
            ApplicationStatus.HIRED
    );

    /** Statuses where the company never got back to the candidate. */
    private static final Set<ApplicationStatus> NO_RESPONSE =
            EnumSet.of(ApplicationStatus.APPLIED, ApplicationStatus.GHOSTED, ApplicationStatus.WITHDRAWN);

    private static final Set<ApplicationStatus> INTERVIEWED = EnumSet.of(
            ApplicationStatus.INTERVIEW, ApplicationStatus.FINAL_INTERVIEW,
            ApplicationStatus.OFFER, ApplicationStatus.HIRED);

    private final ApplicationRepository applicationRepository;

    @Cacheable(cacheNames = CacheNames.STATS_SUMMARY, key = "#userId")
    public StatsResponse.Summary getSummary(UUID userId) {
        Map<ApplicationStatus, Long> counts = countByStatus(userId);

        // Every status in enum order, zeros included, so the response shape is stable
        Map<ApplicationStatus, Long> byStatus = new LinkedHashMap<>();
        for (ApplicationStatus s : ApplicationStatus.values()) {
            byStatus.put(s, counts.getOrDefault(s, 0L));
        }

        long total = sumWhere(byStatus, s -> true);
        long responded = sumWhere(byStatus, s -> !NO_RESPONSE.contains(s));
        long interviewed = sumWhere(byStatus, INTERVIEWED::contains);

        return StatsResponse.Summary.builder()
                .totalApplications(total)
                .byStatus(byStatus)
                .responseRate(ratio(responded, total))
                .interviewConversionRate(ratio(interviewed, responded))
                .build();
    }

    @Cacheable(cacheNames = CacheNames.STATS_TIMELINE, key = "#userId")
    public List<StatsResponse.TimelineEntry> getTimeline(UUID userId) {
        LocalDate currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate firstWeekStart = currentWeekStart.minusWeeks(TIMELINE_WEEKS - 1L);

        // Pre-fill every week with 0 so empty weeks are not missing from the chart
        Map<String, Long> perWeek = new LinkedHashMap<>();
        for (int i = 0; i < TIMELINE_WEEKS; i++) {
            perWeek.put(isoWeek(firstWeekStart.plusWeeks(i)), 0L);
        }

        for (Application app : applicationRepository.findByUserIdAndAppliedAtAfter(userId, firstWeekStart)) {
            perWeek.computeIfPresent(isoWeek(app.getAppliedAt()), (week, count) -> count + 1);
        }

        List<StatsResponse.TimelineEntry> timeline = new ArrayList<>();
        perWeek.forEach((week, count) -> timeline.add(StatsResponse.TimelineEntry.builder()
                .week(week)
                .count(count)
                .build()));
        return timeline;
    }

    /**
     * Cumulative funnel: an application counts towards every pipeline stage up to its current one
     * (a HIRED application also went through APPLIED, INTERVIEW...). Rejected/ghosted/withdrawn
     * applications only count as APPLIED, since there is no status history to know how far they got.
     */
    @Cacheable(cacheNames = CacheNames.STATS_FUNNEL, key = "#userId")
    public List<StatsResponse.FunnelStage> getFunnel(UUID userId) {
        Map<ApplicationStatus, Long> counts = countByStatus(userId);
        long total = sumWhere(counts, s -> true);

        List<StatsResponse.FunnelStage> funnel = new ArrayList<>();
        long previous = 0;
        for (int i = 0; i < PIPELINE.size(); i++) {
            Set<ApplicationStatus> reachedOrBeyond = EnumSet.copyOf(PIPELINE.subList(i, PIPELINE.size()));
            long reached = i == 0 ? total : sumWhere(counts, reachedOrBeyond::contains);

            funnel.add(StatsResponse.FunnelStage.builder()
                    .status(PIPELINE.get(i))
                    .count(reached)
                    .conversionFromPrevious(i == 0 ? ratio(total, total) : ratio(reached, previous))
                    .build());
            previous = reached;
        }
        return funnel;
    }

    private Map<ApplicationStatus, Long> countByStatus(UUID userId) {
        Map<ApplicationStatus, Long> counts = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationRepository.StatusCount row : applicationRepository.countGroupedByStatus(userId)) {
            counts.put(row.getStatus(), row.getTotal());
        }
        return counts;
    }

    private static long sumWhere(Map<ApplicationStatus, Long> counts, Predicate<ApplicationStatus> filter) {
        return counts.entrySet().stream()
                .filter(e -> filter.test(e.getKey()))
                .mapToLong(Map.Entry::getValue)
                .sum();
    }

    private static double ratio(long part, long whole) {
        return whole > 0 ? Math.round((double) part / whole * 100.0) / 100.0 : 0.0;
    }

    /** ISO-8601 week label, e.g. Dec 29 2025 -> "2026-W01". */
    static String isoWeek(LocalDate date) {
        return String.format("%d-W%02d",
                date.get(IsoFields.WEEK_BASED_YEAR),
                date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }
}
