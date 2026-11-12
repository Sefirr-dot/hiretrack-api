package com.sefirr.hiretrack.scheduler;

import com.sefirr.hiretrack.entity.Interview;
import com.sefirr.hiretrack.repository.InterviewRepository;
import com.sefirr.hiretrack.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
@DisallowConcurrentExecution
public class InterviewReminderJob implements Job {

    private final InterviewRepository interviewRepository;
    private final EmailService emailService;
    // Explicit transaction: keeps the session open while the email reads LAZY relations,
    // and does not depend on how Quartz instantiates/proxies the job
    private final TransactionTemplate transactionTemplate;

    @Override
    public void execute(JobExecutionContext context) {
        transactionTemplate.executeWithoutResult(status -> {
            LocalDateTime now = LocalDateTime.now();
            List<Interview> upcoming = interviewRepository.findUpcomingWithoutReminder(now, now.plusHours(24));
            log.info("Running interview reminder job. Found {} interviews to notify.", upcoming.size());

            int sent = 0;
            for (Interview interview : upcoming) {
                // Only flag as sent on success, so failed reminders are retried on the next run
                if (emailService.sendInterviewReminder(interview)) {
                    interview.setReminderSent(true);
                    sent++;
                }
            }
            log.info("Interview reminder job finished: {}/{} reminders sent.", sent, upcoming.size());
        });
    }
}
