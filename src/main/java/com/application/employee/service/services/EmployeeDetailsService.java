package com.application.employee.service.services;

import com.application.employee.service.entities.EmployeeDetails;
import java.util.List;

public interface EmployeeDetailsService {
    EmployeeDetails saveEmployeeDetails(EmployeeDetails details);
    List<EmployeeDetails> getAllEmployeeDetails();
    EmployeeDetails getEmployeeDetailsById(String id);
    EmployeeDetails updateEmployeeDetails(String id,EmployeeDetails updateDetails);
}
