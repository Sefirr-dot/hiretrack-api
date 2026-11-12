package com.sefirr.hiretrack.service;

import com.sefirr.hiretrack.entity.Interview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d 'at' h:mm a", Locale.ENGLISH);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.mail.from:}")
    private String from;

    /**
     * Sends the reminder synchronously so the caller only marks it as sent on success.
     * Must be called while the interview's application and user are loaded (they are LAZY).
     *
     * @return true if the email was handed to the SMTP server
     */
    public boolean sendInterviewReminder(Interview interview) {
        try {
            String company = interview.getApplication().getCompany();
            String scheduledAt = interview.getScheduledAt().format(DATE_FMT);

            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("company", company);
            ctx.setVariable("role", interview.getApplication().getRole());
            ctx.setVariable("interviewType", interview.getType().name().replace("_", " "));
            ctx.setVariable("scheduledAt", scheduledAt);
            ctx.setVariable("platform", interview.getPlatform());
            ctx.setVariable("interviewerName", interview.getInterviewerName());
            ctx.setVariable("durationMinutes", interview.getDurationMinutes());

            String html = templateEngine.process("email/interview-reminder", ctx);
            String userEmail = interview.getApplication().getUser().getEmail();

            MimeMessageHelper helper = new MimeMessageHelper(mailSender.createMimeMessage(), true, "UTF-8");
            if (StringUtils.hasText(from)) {
                helper.setFrom(from);
            }
            helper.setTo(userEmail);
            helper.setSubject("Interview Reminder: " + company + " — " + scheduledAt);
            helper.setText(html, true);

            mailSender.send(helper.getMimeMessage());
            log.info("Reminder sent for interview {}", interview.getId());
            return true;
        } catch (Exception e) {
            log.error("Failed to send reminder for interview {}", interview.getId(), e);
            return false;
        }
    }
}
