package com.application.employee.service.dto;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class EmployeeWithHoldDTO {
    private String firstName;
    private String lastName;
    private String month;
    private String year;
    private String projectName;
    private BigDecimal actualHours;
    private BigDecimal actualRate;
    private BigDecimal actualAmt;
    private BigDecimal paidHours;
    private BigDecimal paidRate;
    private BigDecimal paidAmt;
    private BigDecimal balance;
    private String type;
    private String status;
    private String billrate;
}
