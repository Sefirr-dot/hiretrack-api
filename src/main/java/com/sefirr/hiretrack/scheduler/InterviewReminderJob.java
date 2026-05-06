package com.sefirr.hiretrack.scheduler;

import com.sefirr.hiretrack.entity.Interview;
import com.sefirr.hiretrack.repository.InterviewRepository;
import com.sefirr.hiretrack.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InterviewReminderJob implements Job {

    private final InterviewRepository interviewRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24h = now.plusHours(24);

        List<Interview> upcoming = interviewRepository.findUpcomingWithoutReminder(now, in24h);
        log.info("Running interview reminder job. Found {} interviews to notify.", upcoming.size());

        for (Interview interview : upcoming) {
            emailService.sendInterviewReminder(interview);
            interview.setReminderSent(true);
            interviewRepository.save(interview);
        }
    }
}
