package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.BulkMail;
import com.application.employee.service.repositories.BulkMailRepository;
import com.application.employee.service.services.BulkMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BulkMailServiceImpl implements BulkMailService {

    @Autowired
    private BulkMailRepository bulkMailRepository;

    @Override
    @Transactional
    public BulkMail saveBulkEmail(String recruiterId, BulkMail bulkMail) {
        bulkMail.setRecruiterId(recruiterId);
        return bulkMailRepository.save(bulkMail);
    }

    @Override
    public List<String> getAllEmails() {
        return bulkMailRepository.findEmails();
    }

    @Override
    public List<BulkMail> getEmailsByRecruiterId(String recruiterId) {
        return bulkMailRepository.findByRecruiterId(recruiterId);
    }
}
