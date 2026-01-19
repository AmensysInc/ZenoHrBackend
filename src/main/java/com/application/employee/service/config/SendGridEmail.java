package com.application.employee.service.config;

import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Attachments;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.List;

@Service
public class SendGridEmail {
    @Value("${spring.sendgrid.api-key}")
    private String sendGridApiKey;

    private int lastStatusCode;
    private String lastErrorMessage;

    public int getLastStatusCode() {
        return lastStatusCode;
    }
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void sendEmails(String fromEmail,
                           List<String> toList,
                           List<String> ccList,
                           List<String> bccList,
                           String subject,
                           String body,
                           List<MultipartFile> attachments) throws IOException {

        if (toList == null || toList.isEmpty()) {
            throw new IllegalArgumentException("Recipient (toList) cannot be empty");
        }

        Email from = new Email(fromEmail);
        Content content = new Content("text/html", body); // use HTML format if needed
        SendGrid sg = new SendGrid(sendGridApiKey);

        for (String to : toList) {
            Email toEmail = new Email(to);
            Mail mail = new Mail(from, subject, toEmail, content);

            if (ccList != null) {
                for (String cc : ccList) {
                    mail.personalization.get(0).addCc(new Email(cc));
                }
            }

            if (bccList != null) {
                for (String bcc : bccList) {
                    mail.personalization.get(0).addBcc(new Email(bcc));
                }
            }

            if (attachments != null && !attachments.isEmpty()) {
                for (MultipartFile file : attachments) {
                    Attachments att = new Attachments();
                    att.setContent(Base64.getEncoder().encodeToString(file.getBytes()));
                    att.setType(file.getContentType());
                    att.setFilename(file.getOriginalFilename());
                    att.setDisposition("attachment");
                    mail.addAttachments(att);
                }
            }

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sg.api(request);
            lastStatusCode = response.getStatusCode();
            lastErrorMessage = response.getBody();
            System.out.println(response.getStatusCode());
            System.out.println(response.getBody());
        }
    }

}