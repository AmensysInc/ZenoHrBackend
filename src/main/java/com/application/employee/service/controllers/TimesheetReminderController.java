package com.application.employee.service.controllers;

import com.application.employee.service.dto.TimesheetReminderRequest;
import com.application.employee.service.services.TimesheetReminderService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/timesheets/reminders")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@AllArgsConstructor
public class TimesheetReminderController {

    private final TimesheetReminderService timesheetReminderService;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> sendReminders(@RequestBody TimesheetReminderRequest request) {
        Map<String, Object> result = timesheetReminderService.sendReminders(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> getPendingTimesheets(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        Map<String, Object> result = timesheetReminderService.getPendingTimesheets(month, year);
        return ResponseEntity.ok(result);
    }
}

