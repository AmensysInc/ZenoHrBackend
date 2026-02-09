package com.application.employee.service.controllers;

import com.application.employee.service.dto.PayrollCalculationRequest;
import com.application.employee.service.dto.PayrollGenerateRequest;
import com.application.employee.service.entities.PayrollRecord;
import com.application.employee.service.services.PayrollService;
import com.application.employee.service.services.TaxCalculatorService.TaxCalculations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payroll")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PayrollController {

    @Autowired
    private PayrollService payrollService;

    @PostMapping("/calculate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> calculatePayroll(@RequestBody PayrollCalculationRequest request) {
        try {
            TaxCalculations calculations = payrollService.calculatePayroll(
                    request.getEmployeeId(),
                    request.getGrossPay(),
                    request.getPayPeriodStart(),
                    request.getPayPeriodEnd(),
                    request.getPayDate(),
                    request.getOtherDeductions() != null ? request.getOtherDeductions() : new HashMap<>()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("grossPay", calculations.getGrossPay());
            response.put("federalTax", calculations.getFederalTax());
            response.put("stateTax", calculations.getStateTax());
            response.put("localTax", calculations.getLocalTax());
            response.put("socialSecurity", calculations.getSocialSecurity());
            response.put("medicare", calculations.getMedicare());
            response.put("additionalMedicare", calculations.getAdditionalMedicare());
            response.put("totalTaxes", calculations.getTotalTaxes());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> generatePayroll(@RequestBody PayrollGenerateRequest request) {
        try {
            // Reconstruct TaxCalculations from request
            TaxCalculations taxCalculations = new TaxCalculations();
            taxCalculations.setGrossPay(request.getGrossPay());
            taxCalculations.setFederalTax(request.getFederalTax() != null ? request.getFederalTax() : java.math.BigDecimal.ZERO);
            taxCalculations.setStateTax(request.getStateTax() != null ? request.getStateTax() : java.math.BigDecimal.ZERO);
            taxCalculations.setLocalTax(request.getLocalTax() != null ? request.getLocalTax() : java.math.BigDecimal.ZERO);
            taxCalculations.setSocialSecurity(request.getSocialSecurity() != null ? request.getSocialSecurity() : java.math.BigDecimal.ZERO);
            taxCalculations.setMedicare(request.getMedicare() != null ? request.getMedicare() : java.math.BigDecimal.ZERO);
            taxCalculations.setAdditionalMedicare(request.getAdditionalMedicare() != null ? request.getAdditionalMedicare() : java.math.BigDecimal.ZERO);
            taxCalculations.setTotalTaxes(
                    taxCalculations.getFederalTax()
                            .add(taxCalculations.getStateTax())
                            .add(taxCalculations.getLocalTax())
                            .add(taxCalculations.getSocialSecurity())
                            .add(taxCalculations.getMedicare())
                            .add(taxCalculations.getAdditionalMedicare())
            );

            PayrollRecord payrollRecord = payrollService.generatePayroll(
                    request.getEmployeeId(),
                    request.getGrossPay(),
                    request.getPayPeriodStart(),
                    request.getPayPeriodEnd(),
                    request.getPayDate(),
                    taxCalculations,
                    request.getOtherDeductions() != null ? request.getOtherDeductions() : new HashMap<>(),
                    request.getCustomDeductions() != null ? request.getCustomDeductions() : new HashMap<>()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", payrollRecord);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<List<PayrollRecord>> getAllPayrollRecords() {
        try {
            List<PayrollRecord> records = payrollService.getAllPayrollRecords();
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/records/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<List<PayrollRecord>> getPayrollRecordsByEmployee(@PathVariable String employeeId) {
        try {
            List<PayrollRecord> records = payrollService.getPayrollRecordsByEmployee(employeeId);
            return ResponseEntity.ok(records);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/records/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<PayrollRecord> getPayrollRecordById(@PathVariable Long id) {
        try {
            PayrollRecord record = payrollService.getPayrollRecordById(id);
            return ResponseEntity.ok(record);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/previous-month-payroll/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> getPreviousMonthPayroll(@PathVariable String employeeId) {
        try {
            PayrollRecord record = payrollService.getLatestPayrollRecordByEmployee(employeeId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", record);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

