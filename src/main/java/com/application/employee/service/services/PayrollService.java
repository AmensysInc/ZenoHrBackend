package com.application.employee.service.services;

import com.application.employee.service.entities.PayrollRecord;
import com.application.employee.service.services.TaxCalculatorService.TaxCalculations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface PayrollService {
    
    TaxCalculations calculatePayroll(String employeeId, BigDecimal grossPay, 
                                     LocalDate payPeriodStart, LocalDate payPeriodEnd, 
                                     LocalDate payDate, Map<String, BigDecimal> otherDeductions);
    
    PayrollRecord generatePayroll(String employeeId, BigDecimal grossPay,
                                  LocalDate payPeriodStart, LocalDate payPeriodEnd,
                                  LocalDate payDate, TaxCalculations taxCalculations,
                                  Map<String, BigDecimal> otherDeductions,
                                  Map<String, BigDecimal> customDeductions);
    
    List<PayrollRecord> getAllPayrollRecords();
    
    List<PayrollRecord> getPayrollRecordsByEmployee(String employeeId);
    
    PayrollRecord getPayrollRecordById(Long id);
    
    PayrollRecord getLatestPayrollRecordByEmployee(String employeeId);
}

