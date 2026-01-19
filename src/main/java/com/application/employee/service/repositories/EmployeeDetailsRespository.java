package com.application.employee.service.repositories;

import com.application.employee.service.entities.EmployeeDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeDetailsRespository extends JpaRepository<EmployeeDetails,String> {
}
