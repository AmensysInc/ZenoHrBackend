package com.application.employee.service.controllers;

import com.application.employee.service.enums.VisaStatus;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/visa-status")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@AllArgsConstructor
public class VisaStatusController {

    @GetMapping("/options")
    public ResponseEntity<List<Map<String, String>>> getVisaStatusOptions() {
        List<Map<String, String>> options = Arrays.stream(VisaStatus.values())
                .map(status -> {
                    Map<String, String> option = new java.util.HashMap<>();
                    option.put("value", status.name());
                    option.put("displayName", status.getDisplayName());
                    return option;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(options);
    }
}

