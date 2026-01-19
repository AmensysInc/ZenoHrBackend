package com.application.employee.service.controllers;

import com.application.employee.service.dto.AddressVerificationDTO;
import com.application.employee.service.dto.AddressVerificationRequest;
import com.application.employee.service.dto.AddressVerificationUpdateRequest;
import com.application.employee.service.services.AddressVerificationService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/address-verification")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@AllArgsConstructor
public class AddressVerificationController {

    private final AddressVerificationService addressVerificationService;

    @PostMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<AddressVerificationDTO> createOrUpdateAddressVerification(
            @PathVariable String employeeId,
            @RequestBody AddressVerificationRequest request) {
        AddressVerificationDTO result = addressVerificationService.createOrUpdateAddressVerification(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<AddressVerificationDTO> getAddressVerification(@PathVariable String employeeId) {
        AddressVerificationDTO result = addressVerificationService.getAddressVerificationByEmployeeId(employeeId);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<AddressVerificationDTO> updateAddressVerification(
            @PathVariable String employeeId,
            @RequestBody AddressVerificationUpdateRequest request) {
        AddressVerificationDTO result = addressVerificationService.updateAddressVerification(employeeId, request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{employeeId}/verify-home")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'HR_MANAGER')")
    public ResponseEntity<AddressVerificationDTO> verifyHomeAddress(
            @PathVariable String employeeId,
            @RequestParam String verifiedBy) {
        AddressVerificationDTO result = addressVerificationService.verifyHomeAddress(employeeId, verifiedBy);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{employeeId}/verify-work")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'HR_MANAGER')")
    public ResponseEntity<AddressVerificationDTO> verifyWorkAddress(
            @PathVariable String employeeId,
            @RequestParam String verifiedBy) {
        AddressVerificationDTO result = addressVerificationService.verifyWorkAddress(employeeId, verifiedBy);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'HR_MANAGER')")
    public ResponseEntity<List<AddressVerificationDTO>> getAllAddressVerifications() {
        List<AddressVerificationDTO> result = addressVerificationService.getAllAddressVerifications();
        return ResponseEntity.ok(result);
    }
}

