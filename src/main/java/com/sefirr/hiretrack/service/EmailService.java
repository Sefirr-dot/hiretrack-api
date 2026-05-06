package com.sefirr.hiretrack.service;

import com.sefirr.hiretrack.entity.Interview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("EEEE, MMMM d 'at' h:mm a");

    @Async
    public void sendInterviewReminder(Interview interview) {
        try {
            Context ctx = new Context();
            ctx.setVariable("company", interview.getApplication().getCompany());
            ctx.setVariable("role", interview.getApplication().getRole());
            ctx.setVariable("interviewType", interview.getType().name().replace("_", " "));
            ctx.setVariable("scheduledAt", interview.getScheduledAt().format(DATE_FMT));
            ctx.setVariable("platform", interview.getPlatform());
            ctx.setVariable("interviewerName", interview.getInterviewerName());
            ctx.setVariable("durationMinutes", interview.getDurationMinutes());

            String html = templateEngine.process("email/interview-reminder", ctx);
            String userEmail = interview.getApplication().getUser().getEmail();

            MimeMessageHelper helper = new MimeMessageHelper(mailSender.createMimeMessage(), true, "UTF-8");
            helper.setTo(userEmail);
            helper.setSubject("Interview Reminder: " + interview.getApplication().getCompany()
                    + " — " + interview.getScheduledAt().format(DATE_FMT));
            helper.setText(html, true);

            mailSender.send(helper.getMimeMessage());
            log.info("Reminder sent to {} for interview {}", userEmail, interview.getId());
        } catch (Exception e) {
            log.error("Failed to send reminder for interview {}: {}", interview.getId(), e.getMessage());
        }
    }
}
