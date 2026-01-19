package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Announcement;
import com.application.employee.service.entities.AnnouncementRecipient;
import com.application.employee.service.repositories.AnnouncementRecipientRepository;
import com.application.employee.service.repositories.AnnouncementRepository;
import com.application.employee.service.services.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementRecipientRepository recipientRepository;

    @Override
    public Announcement createAnnouncement(Announcement announcement, List<String> employeeIds) {
        announcement.setCreatedAt(LocalDateTime.now());
        Announcement savedAnnouncement = announcementRepository.save(announcement);

        List<AnnouncementRecipient> recipients = new ArrayList<>();
        for (String empId : employeeIds) {
            AnnouncementRecipient recipient = new AnnouncementRecipient();
            recipient.setAnnouncement(savedAnnouncement);
            recipient.setEmployeeId(empId);
            recipient.setReadStatus(false);
            recipients.add(recipient);
        }
        recipientRepository.saveAll(recipients);
        return savedAnnouncement;
    }

    @Override
    public List<Announcement> getAllAnnouncements() {
        return announcementRepository.findAll();
    }

    @Override
    public List<Announcement> getAnnouncementsForEmployee(String employeeId) {
        List<AnnouncementRecipient> employeeRecipients = recipientRepository.findByEmployeeId(employeeId);
        List<Announcement> announcements = new ArrayList<>();
        for (AnnouncementRecipient r : employeeRecipients) {
            announcements.add(r.getAnnouncement());
        }
        return announcements;
    }

    @Override
    public void markAsRead(Long announcementId, String employeeId) {
        AnnouncementRecipient recipient = recipientRepository.findByAnnouncementIdAndEmployeeId(announcementId, employeeId);
        if (recipient != null) {
            recipient.setReadStatus(true);
            recipientRepository.save(recipient);
        }
    }

    @Override
    public void deleteAnnouncement(Long announcementId) {
        List<AnnouncementRecipient> recipients = recipientRepository.findByAnnouncementId(announcementId);
        recipientRepository.deleteAll(recipients);
        announcementRepository.deleteById(announcementId);
    }
}

