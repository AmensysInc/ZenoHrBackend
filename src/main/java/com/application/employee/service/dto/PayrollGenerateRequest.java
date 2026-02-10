package com.application.employee.service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
public class PayrollGenerateRequest {
    private String employeeId;
    private BigDecimal grossPay;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private LocalDate payDate;
    private BigDecimal federalTax;
    private BigDecimal stateTax;
    private BigDecimal localTax;
    private BigDecimal socialSecurity;
    private BigDecimal medicare;
    private BigDecimal additionalMedicare;
    private BigDecimal netPay;
    private Map<String, BigDecimal> otherDeductions;
    private Map<String, Object> customDeductions; // Can be Map<String, BigDecimal> or Map<String, Object> with name/value
    
    // YTD data from previous payroll (optional - if provided, will be used as starting point)
    private BigDecimal previousYtdGrossPay;
    private BigDecimal previousYtdNetPay;
    private BigDecimal previousYtdFederalTax;
    private BigDecimal previousYtdStateTax;
    private BigDecimal previousYtdLocalTax;
    private BigDecimal previousYtdSocialSecurity;
    private BigDecimal previousYtdMedicare;
}

