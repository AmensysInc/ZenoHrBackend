package com.application.employee.service.controllers;

import com.application.employee.service.config.SendGridEmail;
import com.application.employee.service.entities.EmailRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/email")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class EmailController {
    @Autowired
    private SendGridEmail emailsendgrid;

//    @PostMapping("/send")
//    @PreAuthorize("hasRole('ADMIN') || hasRole('RECRUITER') || hasRole('SADMIN')")
//    public ResponseEntity<String> sendEmails(@ModelAttribute EmailRequest emailRequest,
//                                             @RequestParam(value = "attachment", required = false) MultipartFile[] attachments) {
//        try {
//            if (attachments != null) {
//                emailRequest.setAttachment(List.of(attachments));
//            } else {
//                emailRequest.setAttachment(List.of()); // or set as empty list
//            }
//
//            emailsendgrid.sendEmails(
//                    emailRequest.getFromEmail(),
//                    emailRequest.getToList(),
//                    emailRequest.getCcList(),
//                    emailRequest.getBccList(),
//                    emailRequest.getSubject(),
//                    emailRequest.getBody(),
//                    emailRequest.getAttachment()
//            );
//
//            int statusCode = emailsendgrid.getLastStatusCode();
//            if (statusCode == 202) {
//                return ResponseEntity.ok("Emails sent successfully");
//            } else if (statusCode == 403) {
//                String errorMessage = emailsendgrid.getLastErrorMessage();
//                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorMessage);
//            } else {
//                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error sending emails");
//        }
//    }

    @PostMapping(value = "/send", consumes = "application/json")
    @PreAuthorize("hasRole('ADMIN') || hasRole('RECRUITER') || hasRole('SADMIN')")
    public ResponseEntity<String> sendEmails(@RequestBody EmailRequest emailRequest) {
        try {
            if (emailRequest.getToList() == null || emailRequest.getToList().isEmpty()) {
                return ResponseEntity.badRequest().body("Recipient list (toList) cannot be empty");
            }

            emailsendgrid.sendEmails(
                    emailRequest.getFromEmail(),
                    emailRequest.getToList(),
                    emailRequest.getCcList(),
                    emailRequest.getBccList(),
                    emailRequest.getSubject(),
                    emailRequest.getBody(),
                    List.of()
            );

            int statusCode = emailsendgrid.getLastStatusCode();
            if (statusCode == 202) {
                return ResponseEntity.ok("Emails sent successfully");
            } else if (statusCode == 403) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(emailsendgrid.getLastErrorMessage());
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error sending emails: " + e.getMessage());
        }
    }


}
