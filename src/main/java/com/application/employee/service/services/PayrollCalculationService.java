package com.application.employee.service.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class PayrollCalculationService {
    
    private static final String PAYROLL_ENGINE_PATH = "/app/payroll-engine";
    private static final String CALCULATE_SCRIPT = "calculate.js";
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Calculate payroll using the payroll engine (Node.js) as a library call
     * This calls the Node.js calculation logic directly, not as a separate HTTP service
     */
    public Map<String, Object> calculatePayroll(Map<String, Object> request) {
        try {
            // Execute Node.js calculation script
            ProcessBuilder processBuilder = new ProcessBuilder(
                "node", CALCULATE_SCRIPT
            );
            
            processBuilder.directory(new File(PAYROLL_ENGINE_PATH));
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Write request JSON to stdin
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(objectMapper.writeValueAsString(request));
                writer.flush();
            }
            
            // Read output from stdout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            // Wait for process to complete (max 30 seconds)
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Payroll calculation timed out");
            }
            
            if (process.exitValue() != 0) {
                throw new RuntimeException("Payroll calculation failed: " + output.toString());
            }
            
            // Parse JSON result
            String resultJson = output.toString().trim();
            Map<String, Object> result = objectMapper.readValue(resultJson, Map.class);
            
            // Check for error in result
            if (result.containsKey("error")) {
                throw new RuntimeException("Payroll calculation error: " + result.get("error"));
            }
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Error calculating payroll: " + e.getMessage(), e);
        }
    }
}

