package com.boilerplate.springauth.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class MailService {

    private static final Logger logger = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;
    @Value("${api.mail.from}")
    private String from;
    @Value("${api.security.reset-password.url}")
    private String resetPasswordUrl;

    public MailService (JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendPasswordResetEmail (String to, String token, Long expireMinutes) {

        String resetLink = UriComponentsBuilder.fromUriString(resetPasswordUrl)
                            .queryParam("token", token)
                            .toUriString();

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(from);
        message.setTo(to);
        message.setSubject("Password reset request");
        message.setText(
            "We received a request to reset your password.\n\n" +
            "Click the link below to choose a new password:\n" +
            resetLink + "\n\n" +
            "The link expires in " + expireMinutes + " minutes.\n\n" +
            "If you did not request this, you can ignore this email."
        );

        try {
            mailSender.send(message);
        } catch (MailException exception) {
            logger.error("Failed to send password reset email", exception);
        }

    }

}
