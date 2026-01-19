package com.application.employee.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TimesheetReminderRequest {
    private List<String> employeeIds; // If null, send to all employees with pending timesheets
    private String subject;
    private String message;
    private Integer month; // Optional: filter by month
    private Integer year; // Optional: filter by year
}

