package com.application.employee.service.controllers;

import com.application.employee.service.entities.CheckSettings;
import com.application.employee.service.services.CheckSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/check-settings")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CheckSettingsController {

    @Autowired
    private CheckSettingsService checkSettingsService;

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> getAllCheckSettings() {
        try {
            List<CheckSettings> settings = checkSettingsService.getAllCheckSettings();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", settings);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> getCheckSettingsByCompany(@PathVariable Integer companyId) {
        try {
            CheckSettings settings = checkSettingsService.getCheckSettingsByCompanyId(companyId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", settings);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PutMapping("/company/{companyId}/check-number")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'HR_MANAGER')")
    public ResponseEntity<Map<String, Object>> updateCheckNumber(
            @PathVariable Integer companyId,
            @RequestBody Map<String, Long> request) {
        try {
            Long checkNumber = request.get("checkNumber");
            if (checkNumber == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("error", "checkNumber is required");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            CheckSettings settings = checkSettingsService.updateCheckNumber(companyId, checkNumber);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", settings);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}

