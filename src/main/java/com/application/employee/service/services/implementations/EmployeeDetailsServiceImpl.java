package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.EmployeeDetails;
import com.application.employee.service.exceptions.ResourceNotFoundException;
import com.application.employee.service.repositories.EmployeeDetailsRespository;
import com.application.employee.service.services.EmployeeDetailsService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

public class EmployeeDetailsServiceImpl implements EmployeeDetailsService {
    @Autowired
    private EmployeeDetailsRespository employeeDetailsRespository;
    @Override
    public EmployeeDetails saveEmployeeDetails(EmployeeDetails details) {
        String randomId = UUID.randomUUID().toString();
        details.setEmployeeDetailsID(randomId);
        EmployeeDetails savedEmployeeDetails = employeeDetailsRespository.save(details);
        return savedEmployeeDetails;
    }

    @Override
    public List<EmployeeDetails> getAllEmployeeDetails() {
        return employeeDetailsRespository.findAll();
    }

    @Override
    public EmployeeDetails getEmployeeDetailsById(String id) {
        return employeeDetailsRespository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Employee Details not found with given ID: " + id)
        );
    }

    @Override
    public EmployeeDetails updateEmployeeDetails(String id, EmployeeDetails updateDetails) {
        EmployeeDetails existingDetails = getEmployeeDetailsById(id);
        existingDetails.setFatherName(updateDetails.getFatherName());
        existingDetails.setSsn(updateDetails.getSsn());
        existingDetails.setCurrentWorkLocation(updateDetails.getCurrentWorkLocation());
        return employeeDetailsRespository.save(existingDetails);

    }
}
