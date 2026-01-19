package com.application.employee.service.Util;

import com.application.employee.service.dto.EmployeeDTO;
import com.application.employee.service.entities.Companies;
import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.EmployeeDetails;

public class MapperUtil {
    
    public static Employee convertEmployeeDTO(EmployeeDTO employeeDTO){
        Employee employee = new Employee();

        employee.setEmployeeID(employeeDTO.getEmployeeID());
        employee.setFirstName(employeeDTO.getFirstName());
        employee.setLastName(employeeDTO.getLastName());
        employee.setEmailID(employeeDTO.getEmailID());
        employee.setDob(employeeDTO.getDob());
        employee.setPhoneNo(employeeDTO.getPhoneNo());
        employee.setClgOfGrad(employeeDTO.getClgOfGrad());
        employee.setOnBench(employeeDTO.getOnBench());
        employee.setSecurityGroup(employeeDTO.getSecurityGroup());
        if (employeeDTO.getCompanyId() != null) {
            Companies company = new Companies();
            company.setCompanyId(employeeDTO.getCompanyId());
            employee.setCompany(company);
        }
        employee.setPassword(employeeDTO.getPassword());
        return  employee;
    }
    public static void updateEmployeeFromDTO(EmployeeDTO employeeDTO, Employee employee) {
        if (employeeDTO == null || employee == null) {
            throw new IllegalArgumentException("Both employeeDTO and employee must not be null");
        }
        employee.setFirstName(employeeDTO.getFirstName());
        employee.setLastName(employeeDTO.getLastName());
        employee.setEmailID(employeeDTO.getEmailID());
        employee.setDob(employeeDTO.getDob());
        employee.setPhoneNo(employeeDTO.getPhoneNo());
        employee.setClgOfGrad(employeeDTO.getClgOfGrad());
        employee.setOnBench(employeeDTO.getOnBench());
        employee.setSecurityGroup(employeeDTO.getSecurityGroup());
        if (employeeDTO.getCompanyId() != null) {
            Companies company = new Companies();
            company.setCompanyId(employeeDTO.getCompanyId());
            employee.setCompany(company);
        }
        employee.setPassword(employeeDTO.getPassword());
    }
}
