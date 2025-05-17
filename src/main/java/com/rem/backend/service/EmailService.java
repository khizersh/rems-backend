package com.rem.backend.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    public CompletableFuture<Void> sendEmailAsync(String toEmail, String username, String password) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendCredentialsEmail(toEmail, username, password);
            } catch (Exception e) {
                e.printStackTrace(); // Optionally log this properly
                throw new RuntimeException("Failed to send email", e);
            }
        });
    }

    public void sendCredentialsEmail(String toEmail, String username, String password) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("Your Account Credentials");

        String content = "<p>Hello,</p>"
                + "<p>Your account has been created successfully. Here are your credentials:</p>"
                + "<ul>"
                + "<li><strong>Username:</strong> " + username + "</li>"
                + "<li><strong>Password:</strong> " + password + "</li>"
                + "</ul>"
                + "<p>Please change your password after your first login.</p>"
                + "<p>Regards,<br/>Real Estate Management Team</p>";

        helper.setText(content, true); // true = HTML

        mailSender.send(message);
    }
}