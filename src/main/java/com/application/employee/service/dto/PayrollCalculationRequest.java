package com.application.employee.service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
public class PayrollCalculationRequest {
    private String employeeId;
    private BigDecimal grossPay;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private LocalDate payDate;
    private Map<String, BigDecimal> otherDeductions;
    private Map<String, Object> customDeductions; // For custom deductions with names
}

