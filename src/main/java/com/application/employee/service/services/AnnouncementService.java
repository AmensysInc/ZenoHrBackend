package com.application.employee.service.services;


import com.application.employee.service.entities.Announcement;

import java.util.List;
import java.util.UUID;

public interface AnnouncementService {
    Announcement createAnnouncement(Announcement announcement, List<String> employeeIds);
    List<Announcement> getAllAnnouncements();
    List<Announcement> getAnnouncementsForEmployee(String employeeId);
    void markAsRead(Long announcementId, String employeeId);
    void deleteAnnouncement(Long announcementId);
    }
