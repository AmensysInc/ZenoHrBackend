package com.application.employee.service.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
public class PreviousMonthTaxRequest {
    private String employeeId;
    private LocalDate periodStartDate;
    private LocalDate periodEndDate;
    private BigDecimal federalTaxWithheld;
    private BigDecimal stateTaxWithheld;
    private String stateTaxName;
    private BigDecimal localTaxWithheld;
    private BigDecimal socialSecurityWithheld;
    private BigDecimal medicareWithheld;
    private BigDecimal totalGrossPay;
    private BigDecimal totalNetPay;
    private BigDecimal h1bWage;
    private BigDecimal h1bPrevailingWage;
    private Map<String, Object> additionalFields;
}

