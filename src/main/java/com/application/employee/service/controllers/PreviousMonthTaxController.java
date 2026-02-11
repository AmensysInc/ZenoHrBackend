package com.application.employee.service.controllers;

import com.application.employee.service.dto.PreviousMonthTaxRequest;
import com.application.employee.service.entities.PreviousMonthTax;
import com.application.employee.service.services.PDFParsingService;
import com.application.employee.service.services.PaystubService;
import com.application.employee.service.services.PreviousMonthTaxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

    @Autowired
    private PaystubService paystubService;

    @Value("${file.upload.path:D:\\My Drive\\New folder}")
    private String uploadPath;

    private final ObjectMapper objectMapper;

    public PreviousMonthTaxController() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

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
            
            // Set YTD values
            taxData.setYtdGrossPay(request.getYtdGrossPay());
            taxData.setYtdNetPay(request.getYtdNetPay());
            taxData.setYtdFederalTax(request.getYtdFederalTax());
            taxData.setYtdStateTax(request.getYtdStateTax());
            taxData.setYtdLocalTax(request.getYtdLocalTax());
            taxData.setYtdSocialSecurity(request.getYtdSocialSecurity());
            taxData.setYtdMedicare(request.getYtdMedicare());

            // Convert additionalFields to JSON
            if (request.getAdditionalFields() != null && !request.getAdditionalFields().isEmpty()) {
                taxData.setAdditionalFieldsJson(objectMapper.writeValueAsString(request.getAdditionalFields()));
            }

            // Save PDF file if provided
            if (pdfFile != null && !pdfFile.isEmpty()) {
                try {
                    // Determine the base upload path
                    String basePath = uploadPath;
                    
                    // Check if we're in a Docker/Linux environment
                    String osName = System.getProperty("os.name", "").toLowerCase();
                    boolean isLinux = osName.contains("linux") || osName.contains("unix");
                    
                    // If uploadPath contains Windows drive letter (like D:\) and we're on Linux, use /app/uploads
                    if (isLinux && basePath != null && basePath.contains(":")) {
                        // Extract the path part after the drive letter
                        String pathPart = basePath.substring(basePath.indexOf(":") + 1).trim();
                        pathPart = pathPart.replace("\\", "/").replace("//", "/");
                        // Remove leading slash
                        if (pathPart.startsWith("/")) {
                            pathPart = pathPart.substring(1);
                        }
                        // Use /app/uploads as base in Docker
                        basePath = "/app/uploads/" + pathPart;
                    } else if (isLinux && (basePath == null || basePath.trim().isEmpty() || basePath.contains(":"))) {
                        // Default to /app/uploads in Docker if path is invalid
                        basePath = "/app/uploads";
                    } else {
                        // Windows or valid path - normalize separators
                        basePath = basePath.replace("/", "\\");
                    }
                    
                    // Create base directory first if it doesn't exist
                    Path baseDir = Paths.get(basePath);
                    if (!Files.exists(baseDir)) {
                        Files.createDirectories(baseDir);
                    }
                    
                    // Create the directory path for this employee's tax documents
                    Path taxDir = Paths.get(basePath, "previous-month-tax", request.getEmployeeId());
                    
                    // Create directories if they don't exist
                    if (!Files.exists(taxDir)) {
                        Files.createDirectories(taxDir);
                    }
                    
                    // Get or generate filename
                    String fileName = pdfFile.getOriginalFilename();
                    if (fileName == null || fileName.trim().isEmpty()) {
                        fileName = "paystub_" + System.currentTimeMillis() + ".pdf";
                    }
                    // Sanitize filename
                    fileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                    
                    // Save the file
                    Path filePath = taxDir.resolve(fileName);
                    Files.write(filePath, pdfFile.getBytes());
                    
                    // Store path with forward slashes for consistency (works on both Windows and Unix)
                    taxData.setPdfFilePath(filePath.toString().replace("\\", "/"));
                    taxData.setPdfFileName(fileName);
                } catch (Exception e) {
                    // Log the full error for debugging but don't fail the entire request
                    // PDF is optional - save the tax data even if PDF save fails
                    System.err.println("Warning: Failed to save PDF file: " + e.getMessage());
                    e.printStackTrace();
                    // Continue without PDF - the tax data will still be saved
                }
            }

            PreviousMonthTax saved = previousMonthTaxService.savePreviousMonthTax(request.getEmployeeId(), taxData);

            // Also save the PDF as a Paystub record if PDF was uploaded
            if (pdfFile != null && !pdfFile.isEmpty() && taxData.getPdfFilePath() != null) {
                try {
                    // Read the saved PDF file to create a MultipartFile for Paystub service
                    Path savedPdfPath = Paths.get(taxData.getPdfFilePath());
                    if (Files.exists(savedPdfPath)) {
                        byte[] fileBytes = Files.readAllBytes(savedPdfPath);
                        String fileName = taxData.getPdfFileName() != null ? taxData.getPdfFileName() : pdfFile.getOriginalFilename();
                        if (fileName == null || fileName.isEmpty()) {
                            fileName = "paystub_" + System.currentTimeMillis() + ".pdf";
                        }
                        
                        // Create a MultipartFile implementation from the saved file
                        MultipartFile paystubFile = new MultipartFile() {
                            @Override
                            public String getName() {
                                return "file";
                            }

                            @Override
                            public String getOriginalFilename() {
                                return fileName;
                            }

                            @Override
                            public String getContentType() {
                                return "application/pdf";
                            }

                            @Override
                            public boolean isEmpty() {
                                return fileBytes == null || fileBytes.length == 0;
                            }

                            @Override
                            public long getSize() {
                                return fileBytes != null ? fileBytes.length : 0;
                            }

                            @Override
                            public byte[] getBytes() throws IOException {
                                return fileBytes;
                            }

                            @Override
                            public java.io.InputStream getInputStream() throws IOException {
                                return new java.io.ByteArrayInputStream(fileBytes);
                            }

                            @Override
                            public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
                                java.nio.file.Files.write(dest.toPath(), fileBytes);
                            }
                        };
                        
                        // Extract year from period end date
                        Integer year = taxData.getPeriodEndDate() != null ? taxData.getPeriodEndDate().getYear() : java.time.LocalDate.now().getYear();
                        
                        // Use check date as period end date if available, otherwise use period end date
                        java.time.LocalDate checkDate = taxData.getPeriodEndDate() != null ? taxData.getPeriodEndDate() : java.time.LocalDate.now();
                        
                        // Save as Paystub record
                        paystubService.uploadPaystub(
                            request.getEmployeeId(),
                            paystubFile,
                            year,
                            taxData.getPeriodStartDate(),
                            taxData.getPeriodEndDate(),
                            checkDate,
                            taxData.getTotalGrossPay(),
                            taxData.getTotalNetPay(),
                            taxData.getYtdGrossPay(),
                            taxData.getYtdNetPay(),
                            taxData.getYtdFederalTax(),
                            taxData.getYtdStateTax(),
                            taxData.getYtdLocalTax(),
                            taxData.getYtdSocialSecurity(),
                            taxData.getYtdMedicare(),
                            "System" // Mark as uploaded by system from previous month tax
                        );
                        System.out.println("PDF saved as Paystub record for employee: " + request.getEmployeeId());
                    }
                } catch (Exception e) {
                    // Log error but don't fail the request - PreviousMonthTax is already saved
                    System.err.println("Warning: Failed to save PDF as Paystub record: " + e.getMessage());
                    e.printStackTrace();
                }
            }

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> deletePreviousMonthTax(@PathVariable Long id) {
        try {
            previousMonthTaxService.deletePreviousMonthTax(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Previous month tax record deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

