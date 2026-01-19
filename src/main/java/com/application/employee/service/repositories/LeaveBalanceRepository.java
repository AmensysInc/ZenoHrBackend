package com.application.employee.service.repositories;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.LeaveBalance;
import com.application.employee.service.entities.LeaveType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {
    Optional<LeaveBalance> findByEmployeeAndLeaveType(Employee employee, LeaveType leaveType);
    List<LeaveBalance> findByEmployee_EmployeeID(String employeeId);

}
