package com.sefirr.hiretrack.service;

import com.sefirr.hiretrack.dto.response.StatsResponse;
import com.sefirr.hiretrack.entity.Application;
import com.sefirr.hiretrack.enums.ApplicationStatus;
import com.sefirr.hiretrack.repository.ApplicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceTest {

    @Mock ApplicationRepository applicationRepository;
    @InjectMocks StatsService statsService;

    private final UUID userId = UUID.randomUUID();

    private static ApplicationRepository.StatusCount count(ApplicationStatus status, long total) {
        return new ApplicationRepository.StatusCount() {
            @Override public ApplicationStatus getStatus() { return status; }
            @Override public long getTotal() { return total; }
        };
    }

    @Test
    void isoWeek_usesWeekBasedYear() {
        // Dec 29 2025 is a Monday that belongs to ISO week 1 of 2026
        assertThat(StatsService.isoWeek(LocalDate.of(2025, 12, 29))).isEqualTo("2026-W01");
        // Jan 1 2027 is a Friday that still belongs to ISO week 53 of 2026
        assertThat(StatsService.isoWeek(LocalDate.of(2027, 1, 1))).isEqualTo("2026-W53");
        assertThat(StatsService.isoWeek(LocalDate.of(2026, 3, 4))).isEqualTo("2026-W10");
    }

    @Test
    void summary_computesRatesAndIncludesEveryStatus() {
        when(applicationRepository.countGroupedByStatus(userId)).thenReturn(List.of(
                count(ApplicationStatus.APPLIED, 4),
                count(ApplicationStatus.GHOSTED, 2),
                count(ApplicationStatus.REJECTED, 2),
                count(ApplicationStatus.INTERVIEW, 1),
                count(ApplicationStatus.HIRED, 1)
        ));

        StatsResponse.Summary summary = statsService.getSummary(userId);

        assertThat(summary.getTotalApplications()).isEqualTo(10);
        assertThat(summary.getByStatus()).hasSize(ApplicationStatus.values().length);
        assertThat(summary.getByStatus().get(ApplicationStatus.OFFER)).isZero();
        // responded = REJECTED + INTERVIEW + HIRED = 4 of 10
        assertThat(summary.getResponseRate()).isEqualTo(0.4);
        // interviewed = INTERVIEW + HIRED = 2 of 4 responded
        assertThat(summary.getInterviewConversionRate()).isEqualTo(0.5);
    }

    @Test
    void summary_withNoApplications_returnsZeros() {
        when(applicationRepository.countGroupedByStatus(userId)).thenReturn(List.of());

        StatsResponse.Summary summary = statsService.getSummary(userId);

        assertThat(summary.getTotalApplications()).isZero();
        assertThat(summary.getResponseRate()).isZero();
        assertThat(summary.getInterviewConversionRate()).isZero();
    }

    @Test
    void funnel_isCumulative() {
        when(applicationRepository.countGroupedByStatus(userId)).thenReturn(List.of(
                count(ApplicationStatus.APPLIED, 5),
                count(ApplicationStatus.REJECTED, 3),
                count(ApplicationStatus.INTERVIEW, 1),
                count(ApplicationStatus.HIRED, 1)
        ));

        List<StatsResponse.FunnelStage> funnel = statsService.getFunnel(userId);

        assertThat(funnel).extracting(StatsResponse.FunnelStage::getStatus).containsExactlyElementsOf(StatsService.PIPELINE);
        // APPLIED counts everything; later stages count apps that reached them or went further
        assertThat(funnel).extracting(StatsResponse.FunnelStage::getCount).containsExactly(10L, 2L, 2L, 2L, 1L, 1L, 1L);
        assertThat(funnel.get(0).getConversionFromPrevious()).isEqualTo(1.0);
        assertThat(funnel.get(1).getConversionFromPrevious()).isEqualTo(0.2);
        assertThat(funnel.get(4).getConversionFromPrevious()).isEqualTo(0.5);
        // No stage can ever convert more than 100%
        assertThat(funnel).allSatisfy(stage -> assertThat(stage.getConversionFromPrevious()).isBetween(0.0, 1.0));
    }

    @Test
    void timeline_hasTwelveWeeks_includingEmptyOnes() {
        Application thisWeek = Application.builder().appliedAt(LocalDate.now()).build();
        when(applicationRepository.findByUserIdAndAppliedAtAfter(eq(userId), any())).thenReturn(List.of(thisWeek, thisWeek));

        List<StatsResponse.TimelineEntry> timeline = statsService.getTimeline(userId);

        assertThat(timeline).hasSize(StatsService.TIMELINE_WEEKS);
        assertThat(timeline.get(timeline.size() - 1).getWeek()).isEqualTo(StatsService.isoWeek(LocalDate.now()));
        assertThat(timeline.get(timeline.size() - 1).getCount()).isEqualTo(2);
        assertThat(timeline.subList(0, timeline.size() - 1)).allSatisfy(entry -> assertThat(entry.getCount()).isZero());
    }
}
