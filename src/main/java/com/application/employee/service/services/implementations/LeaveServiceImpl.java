package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.LeaveApplication;
import com.application.employee.service.entities.LeaveBalance;
import com.application.employee.service.entities.LeaveType;
import com.application.employee.service.repositories.EmployeeRespository;
import com.application.employee.service.repositories.LeaveApplicationRepository;
import com.application.employee.service.repositories.LeaveBalanceRepository;
import com.application.employee.service.repositories.LeaveTypeRepository;
import com.application.employee.service.services.LeaveService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveServiceImpl implements LeaveService {

    @Autowired
    private LeaveApplicationRepository leaveApplicationRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    @Autowired
    private EmployeeRespository employeeRepository;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Override
    @Transactional
    public LeaveApplication applyLeave(String employeeId, Long leaveTypeId, LocalDate startDate, LocalDate endDate, String reason) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        LeaveType leaveType = leaveTypeRepository.findById(leaveTypeId)
                .orElseThrow(() -> new RuntimeException("Leave type not found"));

        long daysRequested = startDate.until(endDate).getDays() + 1;

        LeaveBalance leaveBalance = leaveBalanceRepository.findByEmployeeAndLeaveType(employee, leaveType)
                .orElseGet(() -> {
                    // Create a new leave balance using defaultDays from LeaveType
                    LeaveBalance newBalance = new LeaveBalance();
                    newBalance.setEmployee(employee);
                    newBalance.setLeaveType(leaveType);
                    newBalance.setAvailableDays(leaveType.getDefaultDays());
                    newBalance.setBookedDays(0);
                    return leaveBalanceRepository.save(newBalance);
                });

        if (leaveBalance.getAvailableDays() < daysRequested) {
            throw new RuntimeException("Insufficient leave balance");
        }

        leaveBalance.setAvailableDays(leaveBalance.getAvailableDays() - (int) daysRequested);
        leaveBalance.setBookedDays(leaveBalance.getBookedDays() + (int) daysRequested);
        leaveBalanceRepository.save(leaveBalance);

        LeaveApplication application = new LeaveApplication();
        application.setEmployee(employee);
        application.setLeaveType(leaveType);
        application.setStartDate(startDate);
        application.setEndDate(endDate);
        application.setReason(reason);
        application.setStatus("PENDING");
        application.setAppliedDate(LocalDateTime.now());

        return leaveApplicationRepository.save(application);
    }

    @Override
    public List<LeaveApplication> getLeaveApplicationsByEmployee(String employeeId) {
        return leaveApplicationRepository.findByEmployee_EmployeeID(employeeId);
    }
}
