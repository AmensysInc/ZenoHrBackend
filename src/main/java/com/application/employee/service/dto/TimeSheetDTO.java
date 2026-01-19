package com.application.employee.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.util.List;

@Data
public class TimeSheetDTO {
    private Integer month;
    private Integer year;
    private String employeeId;
    private String projectId;
    private Integer sheetId;
    private Double regularHours;
    private Double overTimeHours;
    private Date date;
    private String status;
    private String notes;
}
