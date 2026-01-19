package com.application.employee.service.services;

import com.application.employee.service.entities.LeaveApplication;

import java.time.LocalDate;
import java.util.List;

public interface LeaveService {
    LeaveApplication applyLeave(String employeeId, Long leaveTypeId, LocalDate startDate, LocalDate endDate, String reason);
    List<LeaveApplication> getLeaveApplicationsByEmployee(String employeeId);
}


