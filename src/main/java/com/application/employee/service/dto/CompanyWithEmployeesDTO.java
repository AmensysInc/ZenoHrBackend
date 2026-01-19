package com.application.employee.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CompanyWithEmployeesDTO {
    private Integer companyId;
    private String companyName;
    private List<EmployeeSummaryDTO> employees;
}
