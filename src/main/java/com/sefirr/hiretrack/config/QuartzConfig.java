package com.sefirr.hiretrack.config;

import com.sefirr.hiretrack.scheduler.InterviewReminderJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean
    public JobDetail interviewReminderJobDetail() {
        return JobBuilder.newJob(InterviewReminderJob.class)
                .withIdentity("interviewReminderJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger interviewReminderTrigger(JobDetail interviewReminderJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(interviewReminderJobDetail)
                .withIdentity("interviewReminderTrigger")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 * * * ?"))
                .build();
    }
}
