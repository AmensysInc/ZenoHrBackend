package com.application.employee.service.services;

import com.application.employee.service.dto.TimesheetReminderRequest;

import java.util.Map;

public interface TimesheetReminderService {
    Map<String, Object> sendReminders(TimesheetReminderRequest request);
    Map<String, Object> getPendingTimesheets(Integer month, Integer year);
}

