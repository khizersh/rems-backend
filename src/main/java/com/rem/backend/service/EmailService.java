package com.rem.backend.service;

import com.rem.backend.entity.organization.Organization;
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

    @Value("${app.login.url}")
    private String loginUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Send email asynchronously with user credentials.
     */
    public CompletableFuture<Void> sendEmailAsync(String toEmail, String username, String password, Organization organization) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendCredentialsEmail(toEmail, username, password, organization);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to send email", e);
            }
        });
    }

    /**
     * Sends the credentials email.
     */
    public void sendCredentialsEmail(String toEmail, String username, String password, Organization org) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("Your Account Credentials - " + org.getName());

        String htmlContent = buildHtmlTemplateForPassword(username, password, org);

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }


    public void sendResetPasswordEmail(String toEmail, String username, String resetLink, Organization org) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject("Reset Link - " + org.getName());

        String htmlContent = buildPasswordResetTemplate(username, resetLink, org);

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

    /**
     * Builds the HTML email template with dynamic values replaced.
     */
    private String buildHtmlTemplateForPassword(String username, String password, Organization org) {
        String template = """
        <!doctype html>
        <html lang="en">
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width,initial-scale=1" />
            <title>Account Details</title>
        </head>
        <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
            <table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="background-color:#f4f6f8;padding:24px 0;">
                <tr>
                    <td align="center">
                        <table role="presentation" cellpadding="0" cellspacing="0" width="600" style="max-width:600px;background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,0.08);">
                            <tr>
                                <td style="padding:28px 32px 8px 32px;text-align:center;background:linear-gradient(180deg,#0b57a4, #0c66c2);color:#ffffff;">
                                    <h1 style="margin:0;font-size:22px;line-height:1.2;font-weight:700;">%ORG_NAME%</h1>
                                    <p style="margin:8px 0 0 0;font-size:13px;line-height:1.4;opacity:0.95;">
                                        %ADDRESS% | %CONTACT%
                                    </p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:18px 32px 0 32px;">
                                    <p style="margin:0 0 12px 0;font-size:15px;color:#111827;">Hello,</p>
                                    <p style="margin:0 0 18px 0;font-size:14px;color:#374151;">
                                        Welcome to <strong>%ORG_NAME%</strong>. Your account has been created successfully.
                                    </p>    
                                    
                                    <p style="margin:0 0 18px 0;font-size:14px;color:#374151;">
                                        You can see all details of your Booked Unit.
                                    </p>

                                    <table role="presentation" cellpadding="0" cellspacing="0" width="100%%" style="border:1px solid #e6eef9;border-radius:6px;background:#fbfdff;padding:14px;">
                                        <tr>
                                            <td style="padding:6px 8px;font-size:13px;color:#0b3a66;font-weight:600;width:120px;">Username</td>
                                            <td style="padding:6px 8px;font-size:13px;color:#0b3a66;">%USERNAME%</td>
                                        </tr>
                                        <tr>
                                            <td style="padding:6px 8px;font-size:13px;color:#0b3a66;font-weight:600;">Password</td>
                                            <td style="padding:6px 8px;font-size:13px;color:#0b3a66;">%PASSWORD%</td>
                                        </tr>
                                    </table>

                                    <div style="margin:20px 0 8px 0;text-align:center;">
                                        <a href="%LOGIN_URL%" style="display:inline-block;padding:12px 20px;border-radius:6px;background:#0b66c2;color:#ffffff;text-decoration:none;font-weight:600;font-size:14px;">
                                            Log in to your account
                                        </a>
                                    </div>

                                    <p style="margin:14px 0 0 0;font-size:13px;color:#6b7280;line-height:1.4;">
                                        <strong>Security tip:</strong> Please change your password after your first login. If you did not request this account, contact us immediately.
                                    </p>
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:18px 32px;">
                                    <hr style="border:none;border-top:1px solid #eef2f7;margin:0;">
                                </td>
                            </tr>
                            <tr>
                                <td style="padding:12px 32px 22px 32px;font-size:13px;color:#6b7280;text-align:center;">
                                    <p style="margin:0 0 8px 0;">Need help? Contact our support at %CONTACT%.</p>
                                    <p style="margin:0;font-size:13px;color:#9ca3af;">
                                        &copy; %YEAR% %ORG_NAME% — <a href="https://www.propertydhoondo.com" style="color:#0b66c2;text-decoration:none;">www.propertydhoondo.com</a>
                                    </p>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </body>
        </html>
        """;

        return template
                .replace("%ORG_NAME%", org.getName() != null ? org.getName() : "Our Company")
                .replace("%ADDRESS%", org.getAddress() != null ? org.getAddress() : "Address not available")
                .replace("%CONTACT%", org.getContactNo() != null ? org.getContactNo() : "Contact unavailable")
                .replace("%USERNAME%", username)
                .replace("%PASSWORD%", password)
                .replace("%LOGIN_URL%", loginUrl)
                .replace("%YEAR%", String.valueOf(java.time.Year.now().getValue()));
    }


    private String buildPasswordResetTemplate(String username, String resetLink, Organization org) {
        String template = """
    <!doctype html>
    <html lang="en">
    <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1" />
        <title>Password Reset Request</title>
    </head>
    <body style="margin:0;padding:0;background-color:#f4f6f8;font-family:Arial,Helvetica,sans-serif;">
        <table role="presentation" cellpadding="0" cellspacing="0" width="100%" style="background-color:#f4f6f8;padding:24px 0;">
            <tr>
                <td align="center">
                    <table role="presentation" cellpadding="0" cellspacing="0" width="600" style="max-width:600px;background:#ffffff;border-radius:8px;overflow:hidden;box-shadow:0 2px 6px rgba(0,0,0,0.08);">
                        <tr>
                            <td style="padding:28px 32px 8px 32px;text-align:center;background:linear-gradient(180deg,#0b57a4,#0c66c2);color:#ffffff;">
                                <h1 style="margin:0;font-size:22px;line-height:1.2;font-weight:700;">%ORG_NAME%</h1>
                                <p style="margin:8px 0 0 0;font-size:13px;line-height:1.4;opacity:0.95;">
                                    %ADDRESS% | %CONTACT%
                                </p>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding:18px 32px 0 32px;">
                                <p style="margin:0 0 12px 0;font-size:15px;color:#111827;">Hello %USERNAME%,</p>
                                <p style="margin:0 0 18px 0;font-size:14px;color:#374151;">
                                    We received a request to reset your password for your <strong>%ORG_NAME%</strong> account.
                                </p>    
                                
                                <p style="margin:0 0 18px 0;font-size:14px;color:#374151;">
                                    Click the button below to reset your password. This link will expire in <strong>24 hours</strong>.
                                </p>

                                <div style="margin:24px 0 20px 0;text-align:center;">
                                    <a href="%RESET_LINK%" style="display:inline-block;padding:12px 22px;border-radius:6px;background:#0b66c2;color:#ffffff;text-decoration:none;font-weight:600;font-size:14px;">
                                        Reset Your Password
                                    </a>
                                </div>

                                <p style="margin:14px 0 0 0;font-size:13px;color:#6b7280;line-height:1.4;">
                                    If you didn’t request a password reset, you can safely ignore this email. 
                                    Your password will remain unchanged.
                                </p>
                            </td>
                        </tr>
                        <tr>
                            <td style="padding:18px 32px;">
                                <hr style="border:none;border-top:1px solid #eef2f7;margin:0;">
                            </td>
                        </tr>
                        <tr>
                            <td style="padding:12px 32px 22px 32px;font-size:13px;color:#6b7280;text-align:center;">
                                <p style="margin:0 0 8px 0;">Need help? Contact our support at %CONTACT%.</p>
                                <p style="margin:0;font-size:13px;color:#9ca3af;">
                                    &copy; %YEAR% %ORG_NAME% — 
                                    <a href="https://www.propertydhoondo.com" style="color:#0b66c2;text-decoration:none;">
                                        www.propertydhoondo.com
                                    </a>
                                </p>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
    </body>
    </html>
    """;

        return template
                .replace("%ORG_NAME%", org.getName() != null ? org.getName() : "Our Company")
                .replace("%ADDRESS%", org.getAddress() != null ? org.getAddress() : "Address not available")
                .replace("%CONTACT%", org.getContactNo() != null ? org.getContactNo() : "Contact unavailable")
                .replace("%USERNAME%", username != null ? username : "User")
                .replace("%RESET_LINK%", resetLink)
                .replace("%YEAR%", String.valueOf(java.time.Year.now().getValue()));
    }
}
