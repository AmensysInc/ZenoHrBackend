package com.application.employee.service.controllers;

import com.application.employee.service.dto.LeaveApplicationRequestDTO;
import com.application.employee.service.entities.LeaveApplication;
import com.application.employee.service.entities.LeaveBalance;
import com.application.employee.service.entities.LeaveType;
import com.application.employee.service.repositories.LeaveBalanceRepository;
import com.application.employee.service.repositories.LeaveTypeRepository;
import com.application.employee.service.services.LeaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leave")
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @Autowired
    private LeaveTypeRepository leaveTypeRepository;

    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;

    // 1. Apply for Leave
    @PostMapping("/apply")
    public LeaveApplication applyLeave(@RequestBody LeaveApplicationRequestDTO request) {
        return leaveService.applyLeave(
                request.getEmployeeId(),
                request.getLeaveTypeId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getReason()
        );
    }

    // 2. Get leave applications of an employee
    @GetMapping("/applications/{employeeId}")
    public List<LeaveApplication> getApplicationsByEmployee(@PathVariable String employeeId) {
        return leaveService.getLeaveApplicationsByEmployee(employeeId);
    }

    // 3. Get all available leave types
    @GetMapping("/types")
    public List<LeaveType> getAllLeaveTypes() {
        return leaveTypeRepository.findAll();
    }

    // 4. Add a new leave type
    @PostMapping("/types")
    public LeaveType addLeaveType(@RequestBody LeaveType leaveType) {
        return leaveTypeRepository.save(leaveType);
    }

    // 5. Get all leave balances
    @GetMapping("/balances")
    public List<LeaveBalance> getAllLeaveBalances() {
        return leaveBalanceRepository.findAll();
    }

    // 6. Get leave balances by employee
    @GetMapping("/balances/{employeeId}")
    public List<LeaveBalance> getLeaveBalancesByEmployee(@PathVariable String employeeId) {
        return leaveBalanceRepository.findByEmployee_EmployeeID(employeeId);
    }

    // 7. Create or update leave balance
    @PostMapping("/balances")
    public LeaveBalance addOrUpdateLeaveBalance(@RequestBody LeaveBalance leaveBalance) {
        return leaveBalanceRepository.save(leaveBalance);
    }

    // 8. Delete a leave balance record
    @DeleteMapping("/balances/{id}")
    public void deleteLeaveBalance(@PathVariable Long id) {
        leaveBalanceRepository.deleteById(id);
    }

    // 9. Add leave balance for a specific employee
    @PostMapping("/balances/{employeeId}")
    public LeaveBalance addLeaveBalanceToEmployee(
            @PathVariable String employeeId,
            @RequestBody LeaveBalance leaveBalanceRequest
    ) {
        LeaveBalance leaveBalance = new LeaveBalance();
        leaveBalance.setEmployee(leaveBalanceRequest.getEmployee()); // You can also fetch employee by employeeId if needed
        leaveBalance.getEmployee().setEmployeeID(employeeId); // Ensures employeeId is assigned
        leaveBalance.setLeaveType(leaveBalanceRequest.getLeaveType());
        leaveBalance.setAvailableDays(leaveBalanceRequest.getAvailableDays());
        leaveBalance.setBookedDays(0); // defaulting to 0 initially

        return leaveBalanceRepository.save(leaveBalance);
    }

    @PutMapping("/balances/{id}")
    public LeaveBalance updateLeaveBalance(
            @PathVariable Long id,
            @RequestBody LeaveBalance updatedLeaveBalance
    ) {
        return leaveBalanceRepository.findById(id).map(existingBalance -> {
            existingBalance.setAvailableDays(updatedLeaveBalance.getAvailableDays());
            existingBalance.setBookedDays(updatedLeaveBalance.getBookedDays());
            existingBalance.setLeaveType(updatedLeaveBalance.getLeaveType());
            existingBalance.setEmployee(updatedLeaveBalance.getEmployee());
            return leaveBalanceRepository.save(existingBalance);
        }).orElseThrow(() -> new RuntimeException("LeaveBalance not found with id " + id));
    }

}
