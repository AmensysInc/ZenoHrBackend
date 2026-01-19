package com.application.employee.service.dto;

import lombok.Getter;

import java.time.LocalDate;
@Getter
public class LeaveApplicationRequestDTO {
    private String employeeId;
    private Long leaveTypeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}

