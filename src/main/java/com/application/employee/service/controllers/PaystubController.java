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
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN')")
    public ResponseEntity<String> uploadPaystub(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("year") Integer year,
            @RequestParam("payPeriodStart") String payPeriodStart,
            @RequestParam("payPeriodEnd") String payPeriodEnd,
            @RequestParam(value = "grossPay", required = false) String grossPayStr,
            @RequestParam(value = "netPay", required = false) String netPayStr) {
        try {
            // Get current user
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByEmail(currentUserEmail)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Parse dates
            LocalDate startDate = LocalDate.parse(payPeriodStart);
            LocalDate endDate = LocalDate.parse(payPeriodEnd);

            // Parse amounts
            BigDecimal grossPay = null;
            BigDecimal netPay = null;
            if (grossPayStr != null && !grossPayStr.isEmpty()) {
                grossPay = new BigDecimal(grossPayStr);
            }
            if (netPayStr != null && !netPayStr.isEmpty()) {
                netPay = new BigDecimal(netPayStr);
            }

            paystubService.uploadPaystub(employeeId, file, year, startDate, endDate, grossPay, netPay, currentUser.getEmail());
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
            if (currentUser.getRole() == Role.EMPLOYEE && !currentUser.getId().equals(employeeId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            List<Paystub> paystubs = paystubService.getPaystubsByEmployee(employeeId);
            return ResponseEntity.ok(paystubs);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN')")
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
            if (currentUser.getRole() == Role.EMPLOYEE && 
                !paystub.getEmployee().getEmployeeID().equals(currentUser.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN')")
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

