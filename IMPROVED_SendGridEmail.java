package com.application.employee.service.config;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class SendGridEmail {
    private static final Logger logger = LoggerFactory.getLogger(SendGridEmail.class);
    
    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = 
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$");

    @Value("${spring.sendgrid.api-key}")
    private String sendGridApiKey;

    /**
     * Custom exception for email sending failures
     */
    public static class EmailSendException extends Exception {
        private final int statusCode;
        
        public EmailSendException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }
        
        public int getStatusCode() {
            return statusCode;
        }
    }

    /**
     * Validates email address format
     */
    private void validateEmail(String email) throws IllegalArgumentException {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be null or empty");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Invalid email address format: " + email);
        }
    }

    /**
     * Validates a list of email addresses
     */
    private void validateEmails(List<String> emails) throws IllegalArgumentException {
        if (emails == null || emails.isEmpty()) {
            throw new IllegalArgumentException("Recipient list (toList) cannot be empty");
        }
        for (String email : emails) {
            validateEmail(email);
        }
    }

    /**
     * Sends emails using SendGrid API with improved error handling and validation
     * 
     * @param fromEmail Sender email address
     * @param toList List of recipient email addresses
     * @param ccList Optional list of CC recipients
     * @param bccList Optional list of BCC recipients
     * @param subject Email subject
     * @param body Email body (HTML format)
     * @param attachments Optional list of file attachments
     * @throws EmailSendException if email sending fails
     * @throws IllegalArgumentException if validation fails
     * @throws IOException if file operations fail
     */
    public void sendEmails(String fromEmail,
                           List<String> toList,
                           List<String> ccList,
                           List<String> bccList,
                           String subject,
                           String body,
                           List<MultipartFile> attachments) 
            throws EmailSendException, IllegalArgumentException, IOException {

        // Validate inputs
        validateEmail(fromEmail);
        validateEmails(toList);
        
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Email subject cannot be empty");
        }
        
        if (body == null || body.trim().isEmpty()) {
            throw new IllegalArgumentException("Email body cannot be empty");
        }

        if (sendGridApiKey == null || sendGridApiKey.trim().isEmpty()) {
            throw new IllegalStateException("SendGrid API key is not configured");
        }

        Email from = new Email(fromEmail.trim());
        Content content = new Content("text/html", body);
        SendGrid sg = new SendGrid(sendGridApiKey);

        // Send email to each recipient
        for (String to : toList) {
            try {
                Email toEmail = new Email(to.trim());
                Mail mail = new Mail(from, subject, toEmail, content);

                // Add CC recipients
                if (ccList != null && !ccList.isEmpty()) {
                    for (String cc : ccList) {
                        validateEmail(cc);
                        mail.personalization.get(0).addCc(new Email(cc.trim()));
                    }
                }

                // Add BCC recipients
                if (bccList != null && !bccList.isEmpty()) {
                    for (String bcc : bccList) {
                        validateEmail(bcc);
                        mail.personalization.get(0).addBcc(new Email(bcc.trim()));
                    }
                }

                // Add attachments
                if (attachments != null && !attachments.isEmpty()) {
                    for (MultipartFile file : attachments) {
                        if (file != null && !file.isEmpty()) {
                            try {
                                Attachments att = new Attachments();
                                att.setContent(Base64.getEncoder().encodeToString(file.getBytes()));
                                att.setType(file.getContentType() != null ? 
                                    file.getContentType() : "application/octet-stream");
                                att.setFilename(file.getOriginalFilename() != null ? 
                                    file.getOriginalFilename() : "attachment");
                                att.setDisposition("attachment");
                                mail.addAttachments(att);
                                logger.debug("Added attachment: {}", file.getOriginalFilename());
                            } catch (IOException e) {
                                logger.warn("Failed to attach file: {}. Error: {}", 
                                    file.getOriginalFilename(), e.getMessage());
                                // Continue without this attachment rather than failing entire email
                            }
                        }
                    }
                }

                // Send email via SendGrid API
                Request request = new Request();
                request.setMethod(Method.POST);
                request.setEndpoint("mail/send");
                request.setBody(mail.build());

                Response response = sg.api(request);
                int statusCode = response.getStatusCode();

                // Log response
                logger.debug("SendGrid response for {}: Status={}, Body={}", 
                    to, statusCode, response.getBody());

                // Check for success
                if (statusCode == 202) {
                    logger.info("Email sent successfully to: {}", to);
                } else {
                    String errorMessage = String.format(
                        "Failed to send email to %s. Status: %d, Response: %s",
                        to, statusCode, response.getBody()
                    );
                    logger.error(errorMessage);
                    throw new EmailSendException(errorMessage, statusCode);
                }

            } catch (IOException e) {
                logger.error("IO error while sending email to {}: {}", to, e.getMessage(), e);
                throw new EmailSendException(
                    "IO error while sending email: " + e.getMessage(), 0);
            } catch (Exception e) {
                logger.error("Unexpected error while sending email to {}: {}", 
                    to, e.getMessage(), e);
                throw new EmailSendException(
                    "Unexpected error while sending email: " + e.getMessage(), 0);
            }
        }

        logger.info("Successfully sent {} email(s) to {} recipient(s)", 
            toList.size(), toList.size());
    }

    /**
     * Convenience method to send email to single recipient
     */
    public void sendEmail(String fromEmail,
                         String toEmail,
                         String subject,
                         String body) throws EmailSendException, IOException {
        sendEmails(fromEmail, List.of(toEmail), null, null, subject, body, List.of());
    }
}
