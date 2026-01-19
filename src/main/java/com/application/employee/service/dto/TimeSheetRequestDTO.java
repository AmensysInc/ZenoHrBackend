package com.application.employee.service.dto;

import lombok.Data;

@Data
public class   TimeSheetRequestDTO {
    private Integer month;
    private Integer year;
    private String employeeId;
    private String projectId;
}
