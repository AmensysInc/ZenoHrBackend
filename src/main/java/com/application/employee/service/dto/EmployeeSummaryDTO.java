package com.application.employee.service.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeSummaryDTO {
    private String employeeID;
    private String firstName;
    private String lastName;
}
