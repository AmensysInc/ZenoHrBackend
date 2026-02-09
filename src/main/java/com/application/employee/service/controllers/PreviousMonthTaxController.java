package com.application.employee.service.controllers;

import com.application.employee.service.dto.PreviousMonthTaxRequest;
import com.application.employee.service.entities.PreviousMonthTax;
import com.application.employee.service.services.PDFParsingService;
import com.application.employee.service.services.PreviousMonthTaxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/payroll/previous-month-tax")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PreviousMonthTaxController {

    @Autowired
    private PreviousMonthTaxService previousMonthTaxService;

    @Autowired
    private PDFParsingService pdfParsingService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> savePreviousMonthTax(@RequestBody PreviousMonthTaxRequest request) {
        try {
            PreviousMonthTax taxData = new PreviousMonthTax();
            taxData.setPeriodStartDate(request.getPeriodStartDate());
            taxData.setPeriodEndDate(request.getPeriodEndDate());
            taxData.setFederalTaxWithheld(request.getFederalTaxWithheld());
            taxData.setStateTaxWithheld(request.getStateTaxWithheld());
            taxData.setStateTaxName(request.getStateTaxName());
            taxData.setLocalTaxWithheld(request.getLocalTaxWithheld());
            taxData.setSocialSecurityWithheld(request.getSocialSecurityWithheld());
            taxData.setMedicareWithheld(request.getMedicareWithheld());
            taxData.setTotalGrossPay(request.getTotalGrossPay());
            taxData.setTotalNetPay(request.getTotalNetPay());
            taxData.setH1bWage(request.getH1bWage());
            taxData.setH1bPrevailingWage(request.getH1bPrevailingWage());

            // Convert additionalFields to JSON
            if (request.getAdditionalFields() != null && !request.getAdditionalFields().isEmpty()) {
                taxData.setAdditionalFieldsJson(objectMapper.writeValueAsString(request.getAdditionalFields()));
            }

            PreviousMonthTax saved = previousMonthTaxService.savePreviousMonthTax(request.getEmployeeId(), taxData);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> getPreviousMonthTax(@PathVariable String employeeId) {
        try {
            Optional<PreviousMonthTax> taxDataOpt = previousMonthTaxService.getPreviousMonthTaxByEmployee(employeeId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", taxDataOpt.orElse(null));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/employee-custom-fields/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> getEmployeeCustomFields(@PathVariable String employeeId) {
        try {
            Map<String, Object> customFields = previousMonthTaxService.getEmployeeCustomFields(employeeId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", customFields);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/parse-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> parsePDF(@RequestParam("pdf") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "No PDF file uploaded");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (!file.getContentType().equals("application/pdf")) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "Invalid file type. Only PDF files are allowed.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            Map<String, Object> extractedData = pdfParsingService.parsePayrollPDF(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", extractedData);
            response.put("message", "PDF parsed successfully");
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error parsing PDF: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Error processing PDF: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

