package com.rem.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import static com.rem.backend.utility.Utility.COMPANY_NAME;


@Service
public class EmailService {


    @Autowired
    private JavaMailSender mailSender;

    public boolean sendEmail(String toEmail, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("youremail@gmail.com");
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);

            mailSender.send(message);

            System.out.println("Email sent successfully to " + toEmail);
            return true;
        } catch (MailException e) {
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendUserCredentialsEmail(String toEmail, String username, String password) {
        boolean flag = false;
        try {
            String emailSubject = "Your Account Details";
            String emailBody = "Hello " + username + ",\n\n" +
                    "Your account has been created successfully.\n\n" +
                    "Username: " + username + "\n" +
                    "Password: " + password + "\n\n" +
                    "Please log in and change your password after first login.\n\n" +
                    "Regards,\n" + COMPANY_NAME;

            flag = sendEmail(toEmail, emailSubject, emailBody);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            return flag;
        }

    }

}
