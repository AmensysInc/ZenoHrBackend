package com.application.employee.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
public class PreviousMonthTaxRequest {
    private String employeeId;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate periodStartDate;
    @JsonFormat(pattern = "yyyy-MM-dd")
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
    
    // YTD (Year-To-Date) values
    private BigDecimal ytdGrossPay;
    private BigDecimal ytdNetPay;
    private BigDecimal ytdFederalTax;
    private BigDecimal ytdStateTax;
    private BigDecimal ytdLocalTax;
    private BigDecimal ytdSocialSecurity;
    private BigDecimal ytdMedicare;
}

