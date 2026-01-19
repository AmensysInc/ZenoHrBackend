package com.application.employee.service.repositories;

import com.application.employee.service.entities.AnnouncementRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnnouncementRecipientRepository extends JpaRepository<AnnouncementRecipient, Long> {
    List<AnnouncementRecipient> findByEmployeeId(String employeeId);
    AnnouncementRecipient findByAnnouncementIdAndEmployeeId(Long announcementId, String employeeId);
    List<AnnouncementRecipient> findByAnnouncementId(Long announcementId);
}

