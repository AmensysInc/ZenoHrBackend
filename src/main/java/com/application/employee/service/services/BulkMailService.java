package com.application.employee.service.services;

import com.application.employee.service.entities.BulkMail;
import com.application.employee.service.entities.Contacts;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface BulkMailService {
    @Transactional
    BulkMail saveBulkEmail(String recruiterId, BulkMail bulkMail);
    List<String> getAllEmails();
    List<BulkMail> getEmailsByRecruiterId(String recruiterId);


}
