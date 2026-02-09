package com.application.employee.service.services;

import com.application.employee.service.entities.PreviousMonthTax;

import java.util.Map;
import java.util.Optional;

import java.util.List;

public interface PreviousMonthTaxService {
    
    PreviousMonthTax savePreviousMonthTax(String employeeId, PreviousMonthTax taxData);
    
    Optional<PreviousMonthTax> getPreviousMonthTaxByEmployee(String employeeId);
    
    Map<String, Object> getEmployeeCustomFields(String employeeId);
    
    List<PreviousMonthTax> getAllPreviousMonthTaxRecords();
    
    void deletePreviousMonthTax(Long id);
}

