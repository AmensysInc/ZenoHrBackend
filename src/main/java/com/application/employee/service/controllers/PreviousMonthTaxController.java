package com.application.employee.service.controllers;

import com.application.employee.service.dto.PreviousMonthTaxRequest;
import com.application.employee.service.entities.PreviousMonthTax;
import com.application.employee.service.services.PDFParsingService;
import com.application.employee.service.services.PreviousMonthTaxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
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

    @Value("${file.upload.path:D:\\My Drive\\New folder}")
    private String uploadPath;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> savePreviousMonthTax(
            @RequestPart("data") String requestJson,
            @RequestPart(value = "pdf", required = false) MultipartFile pdfFile) {
        try {
            PreviousMonthTaxRequest request = objectMapper.readValue(requestJson, PreviousMonthTaxRequest.class);
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

            // Save PDF file if provided
            if (pdfFile != null && !pdfFile.isEmpty()) {
                try {
                    Path taxDir = Paths.get(uploadPath, "previous-month-tax", request.getEmployeeId());
                    if (!Files.exists(taxDir)) {
                        Files.createDirectories(taxDir);
                    }
                    String fileName = pdfFile.getOriginalFilename();
                    Path filePath = taxDir.resolve(fileName);
                    Files.write(filePath, pdfFile.getBytes());
                    taxData.setPdfFilePath(filePath.toString());
                    taxData.setPdfFileName(fileName);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save PDF file: " + e.getMessage(), e);
                }
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

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> getAllPreviousMonthTaxRecords() {
        try {
            List<PreviousMonthTax> records = previousMonthTaxService.getAllPreviousMonthTaxRecords();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", records);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/download-pdf/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Resource> downloadPDF(@PathVariable Long id) {
        try {
            List<PreviousMonthTax> allRecords = previousMonthTaxService.getAllPreviousMonthTaxRecords();
            PreviousMonthTax taxData = allRecords.stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElse(null);
            
            if (taxData == null || taxData.getPdfFilePath() == null) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = Paths.get(taxData.getPdfFilePath());
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileBytes = Files.readAllBytes(filePath);
            ByteArrayResource resource = new ByteArrayResource(fileBytes);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", taxData.getPdfFileName() != null ? taxData.getPdfFileName() : "paystub.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

