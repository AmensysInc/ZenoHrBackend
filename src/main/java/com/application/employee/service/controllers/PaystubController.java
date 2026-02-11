package com.application.employee.service.controllers;

import com.application.employee.service.entities.Paystub;
import com.application.employee.service.repositories.UserRepository;
import com.application.employee.service.services.PaystubService;
import com.application.employee.service.user.Role;
import com.application.employee.service.user.User;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/paystubs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class PaystubController {

    @Autowired
    private PaystubService paystubService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<String> uploadPaystub(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("year") Integer year,
            @RequestParam("payPeriodStart") String payPeriodStart,
            @RequestParam("payPeriodEnd") String payPeriodEnd,
            @RequestParam(value = "checkDate", required = false) String checkDateStr,
            @RequestParam(value = "grossPay", required = false) String grossPayStr,
            @RequestParam(value = "netPay", required = false) String netPayStr,
            @RequestParam(value = "ytdGrossPay", required = false) String ytdGrossPayStr,
            @RequestParam(value = "ytdNetPay", required = false) String ytdNetPayStr,
            @RequestParam(value = "ytdFederalTax", required = false) String ytdFederalTaxStr,
            @RequestParam(value = "ytdStateTax", required = false) String ytdStateTaxStr,
            @RequestParam(value = "ytdLocalTax", required = false) String ytdLocalTaxStr,
            @RequestParam(value = "ytdSocialSecurity", required = false) String ytdSocialSecurityStr,
            @RequestParam(value = "ytdMedicare", required = false) String ytdMedicareStr) {
        try {
            // Get current user
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Parse dates
            LocalDate startDate = LocalDate.parse(payPeriodStart);
            LocalDate endDate = LocalDate.parse(payPeriodEnd);
            LocalDate checkDate = null;
            if (checkDateStr != null && !checkDateStr.isEmpty()) {
                checkDate = LocalDate.parse(checkDateStr);
            }

            // Parse amounts
            BigDecimal grossPay = null;
            BigDecimal netPay = null;
            if (grossPayStr != null && !grossPayStr.isEmpty()) {
                grossPay = new BigDecimal(grossPayStr);
            }
            if (netPayStr != null && !netPayStr.isEmpty()) {
                netPay = new BigDecimal(netPayStr);
            }

            // Parse YTD amounts
            BigDecimal ytdGrossPay = null;
            BigDecimal ytdNetPay = null;
            BigDecimal ytdFederalTax = null;
            BigDecimal ytdStateTax = null;
            BigDecimal ytdLocalTax = null;
            BigDecimal ytdSocialSecurity = null;
            BigDecimal ytdMedicare = null;
            
            if (ytdGrossPayStr != null && !ytdGrossPayStr.isEmpty()) {
                ytdGrossPay = new BigDecimal(ytdGrossPayStr);
            }
            if (ytdNetPayStr != null && !ytdNetPayStr.isEmpty()) {
                ytdNetPay = new BigDecimal(ytdNetPayStr);
            }
            if (ytdFederalTaxStr != null && !ytdFederalTaxStr.isEmpty()) {
                ytdFederalTax = new BigDecimal(ytdFederalTaxStr);
            }
            if (ytdStateTaxStr != null && !ytdStateTaxStr.isEmpty()) {
                ytdStateTax = new BigDecimal(ytdStateTaxStr);
            }
            if (ytdLocalTaxStr != null && !ytdLocalTaxStr.isEmpty()) {
                ytdLocalTax = new BigDecimal(ytdLocalTaxStr);
            }
            if (ytdSocialSecurityStr != null && !ytdSocialSecurityStr.isEmpty()) {
                ytdSocialSecurity = new BigDecimal(ytdSocialSecurityStr);
            }
            if (ytdMedicareStr != null && !ytdMedicareStr.isEmpty()) {
                ytdMedicare = new BigDecimal(ytdMedicareStr);
            }

            paystubService.uploadPaystub(employeeId, file, year, startDate, endDate, checkDate, 
                    grossPay, netPay, ytdGrossPay, ytdNetPay, ytdFederalTax, ytdStateTax, 
                    ytdLocalTax, ytdSocialSecurity, ytdMedicare, currentUser.getEmail());
            return ResponseEntity.ok("Paystub uploaded successfully");
        } catch (FileUploadException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload paystub: " + e.getMessage());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload paystub: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'EMPLOYEE', 'HR_MANAGER')")
    public ResponseEntity<List<Paystub>> getPaystubsByEmployee(@PathVariable String employeeId) {
        try {
            // Get current user
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Employees can only see their own paystubs
            // Also ensure employeeId matches current user's ID
            if (currentUser.getRole() == Role.EMPLOYEE) {
                if (!currentUser.getId().equals(employeeId)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
                // For EMPLOYEE, always use their own ID regardless of what's in the path
                employeeId = currentUser.getId();
            }

            List<Paystub> paystubs = paystubService.getPaystubsByEmployee(employeeId);
            return ResponseEntity.ok(paystubs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<List<Paystub>> getAllPaystubs() {
        try {
            List<Paystub> paystubs = paystubService.getAllPaystubs();
            return ResponseEntity.ok(paystubs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'EMPLOYEE', 'HR_MANAGER')")
    public ResponseEntity<Paystub> getPaystubById(@PathVariable Long id) {
        try {
            Paystub paystub = paystubService.getPaystubById(id);
            
            // Get current user
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Employees can only access their own paystubs
            if (currentUser.getRole() == Role.EMPLOYEE && 
                !paystub.getEmployee().getEmployeeID().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            return ResponseEntity.ok(paystub);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'EMPLOYEE', 'HR_MANAGER')")
    public ResponseEntity<byte[]> downloadPaystub(@PathVariable Long id) {
        try {
            Paystub paystub = paystubService.getPaystubById(id);
            
            // Get current user
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Employees can only download their own paystubs
            if (currentUser.getRole() == Role.EMPLOYEE) {
                String employeeId = paystub.getEmployee().getEmployeeID();
                String userId = currentUser.getId();
                // Ensure employee can only access their own paystubs
                if (!employeeId.equals(userId)) {
                    System.out.println("[PaystubController] EMPLOYEE access denied - employeeId: " + employeeId + ", userId: " + userId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            byte[] fileData = paystubService.downloadPaystub(id);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", paystub.getFileName());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<String> deletePaystub(@PathVariable Long id) {
        try {
            paystubService.deletePaystub(id);
            return ResponseEntity.ok("Paystub deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete paystub: " + e.getMessage());
        }
    }
}

