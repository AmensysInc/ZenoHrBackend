package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.PreviousMonthTax;
import com.application.employee.service.repositories.PreviousMonthTaxRepository;
import com.application.employee.service.services.EmployeeService;
import com.application.employee.service.services.PreviousMonthTaxService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PreviousMonthTaxServiceImpl implements PreviousMonthTaxService {

    @Autowired
    private PreviousMonthTaxRepository previousMonthTaxRepository;

    @Autowired
    private EmployeeService employeeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public PreviousMonthTax savePreviousMonthTax(String employeeId, PreviousMonthTax taxData) {
        Employee employee = employeeService.getEmployee(employeeId);
        if (employee == null) {
            throw new RuntimeException("Employee not found: " + employeeId);
        }

        // Check if previous month tax already exists for this employee
        Optional<PreviousMonthTax> existingOpt = previousMonthTaxRepository.findByEmployeeEmployeeID(employeeId);
        if (existingOpt.isPresent()) {
            PreviousMonthTax existing = existingOpt.get();
            // Update existing record
            existing.setPeriodStartDate(taxData.getPeriodStartDate());
            existing.setPeriodEndDate(taxData.getPeriodEndDate());
            existing.setFederalTaxWithheld(taxData.getFederalTaxWithheld());
            existing.setStateTaxWithheld(taxData.getStateTaxWithheld());
            existing.setStateTaxName(taxData.getStateTaxName());
            existing.setLocalTaxWithheld(taxData.getLocalTaxWithheld());
            existing.setSocialSecurityWithheld(taxData.getSocialSecurityWithheld());
            existing.setMedicareWithheld(taxData.getMedicareWithheld());
            existing.setTotalGrossPay(taxData.getTotalGrossPay());
            existing.setTotalNetPay(taxData.getTotalNetPay());
            existing.setH1bWage(taxData.getH1bWage());
            existing.setH1bPrevailingWage(taxData.getH1bPrevailingWage());
            existing.setAdditionalFieldsJson(taxData.getAdditionalFieldsJson());
            if (taxData.getPdfFilePath() != null) {
                existing.setPdfFilePath(taxData.getPdfFilePath());
            }
            if (taxData.getPdfFileName() != null) {
                existing.setPdfFileName(taxData.getPdfFileName());
            }
            return previousMonthTaxRepository.save(existing);
        } else {
            // Create new record
            taxData.setEmployee(employee);
            return previousMonthTaxRepository.save(taxData);
        }
    }

    @Override
    public Optional<PreviousMonthTax> getPreviousMonthTaxByEmployee(String employeeId) {
        // Get the most recent record by period end date
        java.util.List<PreviousMonthTax> records = previousMonthTaxRepository.findAllByEmployeeEmployeeIDOrderByPeriodEndDateDesc(employeeId);
        if (records != null && !records.isEmpty()) {
            return Optional.of(records.get(0));
        }
        return Optional.empty();
    }

    @Override
    public Map<String, Object> getEmployeeCustomFields(String employeeId) {
        // Get the most recent record by period end date
        java.util.List<PreviousMonthTax> records = previousMonthTaxRepository.findAllByEmployeeEmployeeIDOrderByPeriodEndDateDesc(employeeId);
        if (records == null || records.isEmpty()) {
            return new HashMap<>();
        }
        
        PreviousMonthTax taxData = records.get(0);
        if (taxData.getAdditionalFieldsJson() == null || taxData.getAdditionalFieldsJson().trim().isEmpty()) {
            return new HashMap<>();
        }
        
        try {
            Map<String, Object> additionalFields = objectMapper.readValue(
                    taxData.getAdditionalFieldsJson(),
                    new TypeReference<Map<String, Object>>() {}
            );
            
            // Convert to format expected by frontend
            Map<String, Object> customFields = new HashMap<>();
            for (Map.Entry<String, Object> entry : additionalFields.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fieldData = (Map<String, Object>) entry.getValue();
                    customFields.put(entry.getKey(), fieldData);
                } else {
                    Map<String, Object> fieldData = new HashMap<>();
                    fieldData.put("name", entry.getKey());
                    fieldData.put("defaultValue", entry.getValue());
                    customFields.put(entry.getKey(), fieldData);
                }
            }
            return customFields;
        } catch (Exception e) {
            System.err.println("Error parsing additionalFieldsJson for employee " + employeeId + ": " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    @Override
    public List<PreviousMonthTax> getAllPreviousMonthTaxRecords() {
        return previousMonthTaxRepository.findAllWithEmployee();
    }

    @Override
    @Transactional
    public void deletePreviousMonthTax(Long id) {
        if (!previousMonthTaxRepository.existsById(id)) {
            throw new RuntimeException("Previous month tax record not found: " + id);
        }
        previousMonthTaxRepository.deleteById(id);
    }
}

