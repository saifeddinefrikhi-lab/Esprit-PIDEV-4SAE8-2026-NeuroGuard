package com.neuroguard.userservice.services;

import com.neuroguard.userservice.dto.MailerSendEmailRequest;
import com.neuroguard.userservice.entities.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.mail.from.name:NeuroGuard Support}")
    private String fromName;

    @Value("${app.mail.from.email:noreply@neuroguard.com}")
    private String fromEmailAddress;

    @Value("${mailersend.api-key}")
    private String mailerSendApiKey;

    @Value("${mailersend.api-url:https://api.mailersend.com/v1}")
    private String mailerSendApiUrl;

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Send password reset email to user
     */
    @Async
    public void sendPasswordResetEmail(String recipientEmail, String firstName, String lastName, String resetToken) {
        try {
            log.info("Preparing password reset email for: {}", recipientEmail);
            
            String resetLink = buildResetLink(resetToken);
            String htmlContent = buildPasswordResetEmailContent(firstName, resetLink);
            
            // Construct MailerSend Request
            MailerSendEmailRequest request = new MailerSendEmailRequest();
            
            // Set Sender
            request.setFrom(new MailerSendEmailRequest.Recipient(fromEmailAddress, fromName));
            
            // Set Recipient
            String recipientName = (firstName != null ? firstName : "") + 
                                   (lastName != null ? " " + lastName : "");
            if (recipientName.trim().isEmpty()) recipientName = "User";
            
            request.setTo(Collections.singletonList(new MailerSendEmailRequest.Recipient(recipientEmail, recipientName)));
            
            // Set Subject and Content
            request.setSubject("Reset Your NeuroGuard Password");
            request.setHtml(htmlContent);
            request.setText(buildSimplePasswordResetEmailContent(firstName, resetLink));
            
            // Send via MailerSend
            sendEmailViaMailerSend(request);
            
            log.info("Password reset email sent successfully via MailerSend to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("Error in password reset email process for: {}", recipientEmail, e);
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    /**
     * Internal method to send email via MailerSend API
     */
    private void sendEmailViaMailerSend(MailerSendEmailRequest request) {
        try {
            if (mailerSendApiKey == null || mailerSendApiKey.isEmpty() || mailerSendApiKey.contains("your-api-key")) {
                log.warn("MailerSend API Key is missing or default. Email will not be sent.");
                return;
            }

            log.info("Calling MailerSend API to send email...");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(mailerSendApiKey);

            HttpEntity<MailerSendEmailRequest> entity = new HttpEntity<>(request, headers);
            
            String url = mailerSendApiUrl + "/email";
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("MailerSend API call successful. Response body: {}", response.getBody());
            } else {
                log.error("MailerSend API call failed with status: {}. Response: {}", response.getStatusCode(), response.getBody());
            }
        } catch (HttpStatusCodeException e) {
            log.error("MailerSend API error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Exception while calling MailerSend API: {}", e.getMessage());
        }
    }

    /**
     * Send simple text email (fallback)
     */
    @Async
    public void sendSimplePasswordResetEmail(String recipientEmail, String firstName, String resetToken) {
        try {
            String resetLink = buildResetLink(resetToken);
            String subject = "Reset Your NeuroGuard Password";
            String textContent = buildSimplePasswordResetEmailContent(firstName, resetLink);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(textContent);

            mailSender.send(message);
            log.info("Simple password reset email sent to: {}", recipientEmail);

        } catch (Exception e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    /**
     * Build the password reset link
     */
    private String buildResetLink(String resetToken) {
        return String.format("%s/reset-password?token=%s", frontendUrl, resetToken);
    }

    /**
     * Build HTML email content for password reset
     */
    private String buildPasswordResetEmailContent(String firstName, String resetLink) {
        if (firstName == null) firstName = "User";
        
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>Reset Your NeuroGuard Password</title>\n" +
            "    <style>\n" +
            "        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }\n" +
            "        .container { max-width: 600px; margin: 0 auto; padding: 20px; }\n" +
            "        .header { background: #4a90e2; color: white; padding: 20px; text-align: center; }\n" +
            "        .content { padding: 20px; background: #f9f9f9; }\n" +
            "        .button { display: inline-block; padding: 12px 24px; background: #4a90e2; color: white; text-decoration: none; border-radius: 4px; margin: 20px 0; }\n" +
            "        .footer { text-align: center; padding: 20px; color: #666; font-size: 12px; }\n" +
            "        .security-note { background: #fff3cd; padding: 10px; border-radius: 4px; margin: 10px 0; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>NeuroGuard</h1>\n" +
            "            <h2>Password Reset Request</h2>\n" +
            "        </div>\n" +
            "        <div class=\"content\">\n" +
            "            <p>Hi " + firstName + ",</p>\n" +
            "            <p>We received a request to reset your password for your NeuroGuard account.</p>\n" +
            "            \n" +
            "            <div class=\"security-note\">\n" +
            "                <strong>Security Notice:</strong> If you didn't request this password reset, please ignore this email. Your password will remain unchanged.\n" +
            "            </div>\n" +
            "            \n" +
            "            <p>Click the button below to reset your password:</p>\n" +
            "            \n" +
            "            <div style=\"text-align: center;\">\n" +
            "                <a href=\"" + resetLink + "\" class=\"button\">Reset Password</a>\n" +
            "            </div>\n" +
            "            \n" +
            "            <p>Or copy and paste this link into your browser:</p>\n" +
            "            <p style=\"word-break: break-all; background: #eee; padding: 10px; border-radius: 4px;\">\n" +
            "                " + resetLink + "\n" +
            "            </p>\n" +
            "            \n" +
            "            <p><strong>This link will expire in 30 minutes.</strong></p>\n" +
            "            \n" +
            "            <p>If you have any questions or need assistance, please contact our support team.</p>\n" +
            "            \n" +
            "            <p>Best regards,<br>The NeuroGuard Team</p>\n" +
            "        </div>\n" +
            "        <div class=\"footer\">\n" +
            "            <p>&copy; 2024 NeuroGuard. All rights reserved.</p>\n" +
            "            <p>This is an automated message. Please do not reply to this email.</p>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "</body>\n" +
            "</html>";
    }

    /**
     * Build simple text email content for password reset
     */
    private String buildSimplePasswordResetEmailContent(String firstName, String resetLink) {
        if (firstName == null) firstName = "User";
        
        return "Hi " + firstName + ",\n\n" +
            "We received a request to reset your password for your NeuroGuard account.\n\n" +
            "If you didn't request this password reset, please ignore this email. Your password will remain unchanged.\n\n" +
            "To reset your password, click the link below:\n" +
            resetLink + "\n\n" +
            "This link will expire in 30 minutes.\n\n" +
            "If you have any questions or need assistance, please contact our support team.\n\n" +
            "Best regards,\n" +
            "The NeuroGuard Team\n\n" +
            "---\n" +
            "© 2024 NeuroGuard. All rights reserved.\n" +
            "This is an automated message. Please do not reply to this email.";
    }

    /**
     * Test email configuration
     */
    public boolean testEmailConfiguration() {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(fromEmail); // Send to self for testing
            message.setSubject("NeuroGuard Email Test");
            message.setText("This is a test email to verify email configuration.");
            
            mailSender.send(message);
            log.info("Email configuration test successful");
            return true;
        } catch (Exception e) {
            log.error("Email configuration test failed", e);
            return false;
        }
    }
}
