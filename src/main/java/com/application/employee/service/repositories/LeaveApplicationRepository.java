package com.application.employee.service.repositories;

import com.application.employee.service.entities.LeaveApplication;
import com.application.employee.service.entities.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LeaveApplicationRepository extends JpaRepository<LeaveApplication, Long> {
    List<LeaveApplication> findByEmployee_EmployeeID(String employeeId);

}
