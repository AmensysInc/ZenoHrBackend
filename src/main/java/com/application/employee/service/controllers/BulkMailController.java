package com.application.employee.service.controllers;

import com.application.employee.service.entities.BulkMail;
import com.application.employee.service.services.BulkMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bulkmails")
public class BulkMailController {

    @Autowired
    private BulkMailService bulkMailService;

//    @PostMapping("/save")
//    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER','SADMIN')")
//    public ResponseEntity<List<BulkMail>> saveBulkEmails(@RequestParam String recruiterId, @RequestBody List<BulkMail> bulkMails) {
//        List<BulkMail> savedEmails = bulkMailService.saveBulkEmails(recruiterId, bulkMails);
//        return new ResponseEntity<>(savedEmails, HttpStatus.CREATED);
//    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER','SADMIN')")
    public ResponseEntity<List<String>> getAllEmails() {
        List<String> emailIds = bulkMailService.getAllEmails();
        return new ResponseEntity<>(emailIds, HttpStatus.OK);
    }

    @GetMapping("/{recruiterId}")
    public ResponseEntity<List<BulkMail>> getEmailsByRecruiterId(@PathVariable String recruiterId) {
        List<BulkMail> contacts = bulkMailService.getEmailsByRecruiterId(recruiterId);
        return new ResponseEntity<>(contacts, HttpStatus.OK);
    }
    @PostMapping()
    @PreAuthorize("hasAnyRole('ADMIN', 'RECRUITER','SADMIN')")
    public ResponseEntity<BulkMail> saveBulkEmail(@RequestParam String recruiterId, @RequestBody BulkMail bulkMail) {
        BulkMail savedMail = bulkMailService.saveBulkEmail(recruiterId, bulkMail);
        return new ResponseEntity<>(savedMail, HttpStatus.CREATED);
    }
}
