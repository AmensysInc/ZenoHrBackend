package com.application.employee.service.controllers;

import com.application.employee.service.user.Role;
import com.application.employee.service.user.User;
import com.application.employee.service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Temporary controller to create admin user
 * Remove or secure this endpoint after creating the admin user
 */
@RestController
@RequestMapping("/admin/create-user")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<String> createAdminUser(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false, defaultValue = "Rama") String firstname,
            @RequestParam(required = false, defaultValue = "K") String lastname,
            @RequestParam(required = false, defaultValue = "ADMIN") String role) {
        
        try {
            // Check if user already exists
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest()
                        .body("User with email " + email + " already exists");
            }

            // Create admin user
            User adminUser = User.builder()
                    .id(UUID.randomUUID().toString())
                    .firstname(firstname)
                    .lastname(lastname)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(Role.valueOf(role.toUpperCase()))
                    .tempPassword(null)
                    .build();

            userRepository.save(adminUser);
            
            return ResponseEntity.ok("Admin user created successfully: " + email);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error creating user: " + e.getMessage());
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Object>> checkAdminUser(@RequestParam String email) {
        try {
            var userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("exists", true);
                response.put("email", user.getEmail());
                response.put("role", user.getRole());
                response.put("firstname", user.getFirstname());
                response.put("lastname", user.getLastname());
                response.put("hasPassword", user.getPassword() != null && !user.getPassword().isEmpty());
                response.put("hasTempPassword", user.getTempPassword() != null && !user.getTempPassword().isEmpty());
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new java.util.HashMap<>();
                response.put("exists", false);
                response.put("message", "User not found");
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("exists", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}

