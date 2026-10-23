package com.chiangraipass.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends plain-text email. mode=log (default) only prints the message to the application log,
 * which is for local development; it exposes links, so production must use mode=smtp.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final String mode;
    private final String from;

    public MailService(
            ObjectProvider<JavaMailSender> mailSender,
            @Value("${app.mail.mode:log}") String mode,
            @Value("${app.mail.from:no-reply@ceipass.local}") String from) {
        this.mailSender = mailSender;
        this.mode = mode;
        this.from = from;
    }

    public void send(String to, String subject, String body) {
        if ("smtp".equalsIgnoreCase(mode)) {
            JavaMailSender sender = mailSender.getIfAvailable();
            if (sender == null) {
                throw new IllegalStateException(
                        "MAIL_MODE=smtp but no SMTP server is configured (set SPRING_MAIL_HOST).");
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
        } else {
            log.info("[DEV MAIL] To: {} | Subject: {}\n{}", to, subject, body);
        }
    }
}