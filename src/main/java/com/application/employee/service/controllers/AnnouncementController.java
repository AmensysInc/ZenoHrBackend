package com.application.employee.service.controllers;

import com.application.employee.service.entities.Announcement;
import com.application.employee.service.services.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping
    public ResponseEntity<Announcement> createAnnouncement(@RequestBody Map<String, Object> request) {
        Announcement announcement = new Announcement();
        announcement.setTitle((String) request.get("title"));
        announcement.setMessage((String) request.get("message"));
        announcement.setType((String) request.get("type"));
        announcement.setCreatedBy(request.get("createdBy").toString()); // Accepts UUID or Numeric as String

        List<String> employeeIds = (List<String>) request.get("employeeIds");

        return ResponseEntity.ok(announcementService.createAnnouncement(announcement, employeeIds));
    }

    @GetMapping
    public ResponseEntity<List<Announcement>> getAllAnnouncements() {
        return ResponseEntity.ok(announcementService.getAllAnnouncements());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Announcement>> getAnnouncementsForEmployee(@PathVariable String employeeId) {
        return ResponseEntity.ok(announcementService.getAnnouncementsForEmployee(employeeId));
    }

    @PutMapping("/{announcementId}/mark-read/{employeeId}")
    public ResponseEntity<String> markAsRead(@PathVariable Long announcementId, @PathVariable String employeeId) {
        announcementService.markAsRead(announcementId, employeeId);
        return ResponseEntity.ok("Marked as read");
    }
    @DeleteMapping("/{announcementId}")
    public ResponseEntity<String> deleteAnnouncement(@PathVariable Long announcementId) {
        announcementService.deleteAnnouncement(announcementId);
        return ResponseEntity.ok("Announcement deleted successfully");
    }
}
