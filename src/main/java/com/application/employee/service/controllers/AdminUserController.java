package com.application.employee.service.controllers;

import com.application.employee.service.entities.UserCompanyRole;
import com.application.employee.service.repositories.UserCompanyRoleRepository;
import com.application.employee.service.user.Role;
import com.application.employee.service.user.User;
import com.application.employee.service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
    private final UserCompanyRoleRepository userCompanyRoleRepository;

    @GetMapping
    public ResponseEntity<String> createAdminUser(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false, defaultValue = "Rama") String firstname,
            @RequestParam(required = false, defaultValue = "K") String lastname,
            @RequestParam(required = false, defaultValue = "ADMIN") String role,
            @RequestParam(required = false) Long companyId) {
        
        try {
            // Check if user already exists
            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest()
                        .body("User with email " + email + " already exists");
            }

            Role userRole = Role.valueOf(role.toUpperCase());
            
            // Create admin user
            User adminUser = User.builder()
                    .id(UUID.randomUUID().toString())
                    .firstname(firstname)
                    .lastname(lastname)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(userRole)
                    .tempPassword(null)
                    .build();

            userRepository.save(adminUser);
            
            // ✅ For ADMIN role, create UserCompanyRole if companyId is provided
            // SADMIN should NOT have a default company
            // GROUP_ADMIN can optionally have a company assigned here, or assign later via AddUserRole page
            if (userRole == Role.ADMIN && companyId != null) {
                UserCompanyRole userCompanyRole = new UserCompanyRole();
                userCompanyRole.setUserId(adminUser.getId());
                userCompanyRole.setCompanyId(companyId.intValue());
                userCompanyRole.setRole(Role.ADMIN.name());
                userCompanyRole.setDefaultCompany("true");
                userCompanyRole.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
                
                // Ensure only one default company per user
                List<UserCompanyRole> existingRoles = userCompanyRoleRepository.findByUserId(adminUser.getId());
                for (UserCompanyRole existing : existingRoles) {
                    existing.setDefaultCompany("false");
                    userCompanyRoleRepository.save(existing);
                }
                
                userCompanyRoleRepository.save(userCompanyRole);
                return ResponseEntity.ok("Admin user created successfully: " + email + " assigned to company ID: " + companyId);
            } else if (userRole == Role.SADMIN) {
                // SADMIN should not have a default company
                return ResponseEntity.ok("Super Admin user created successfully: " + email + " (no company assignment)");
            } else if (userRole == Role.GROUP_ADMIN) {
                // GROUP_ADMIN can optionally have a company assigned during creation
                if (companyId != null) {
                    UserCompanyRole userCompanyRole = new UserCompanyRole();
                    userCompanyRole.setUserId(adminUser.getId());
                    userCompanyRole.setCompanyId(companyId.intValue());
                    userCompanyRole.setRole(Role.GROUP_ADMIN.name());
                    userCompanyRole.setDefaultCompany("true");
                    userCompanyRole.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
                    
                    // Ensure only one default company per user
                    List<UserCompanyRole> existingRoles = userCompanyRoleRepository.findByUserId(adminUser.getId());
                    for (UserCompanyRole existing : existingRoles) {
                        existing.setDefaultCompany("false");
                        userCompanyRoleRepository.save(existing);
                    }
                    
                    userCompanyRoleRepository.save(userCompanyRole);
                    return ResponseEntity.ok("Group Admin user created successfully: " + email + " assigned to company ID: " + companyId + ". You can assign additional companies using 'Add User Role' page.");
                } else {
                    return ResponseEntity.ok("Group Admin user created successfully: " + email + ". Please assign companies using 'Add User Role' page.");
                }
            } else if (userRole == Role.REPORTING_MANAGER) {
                // REPORTING_MANAGER can optionally have a company assigned
                if (companyId != null) {
                    UserCompanyRole userCompanyRole = new UserCompanyRole();
                    userCompanyRole.setUserId(adminUser.getId());
                    userCompanyRole.setCompanyId(companyId.intValue());
                    userCompanyRole.setRole(Role.REPORTING_MANAGER.name());
                    userCompanyRole.setDefaultCompany("true");
                    userCompanyRole.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
                    
                    // Ensure only one default company per user
                    List<UserCompanyRole> existingRoles = userCompanyRoleRepository.findByUserId(adminUser.getId());
                    for (UserCompanyRole existing : existingRoles) {
                        existing.setDefaultCompany("false");
                        userCompanyRoleRepository.save(existing);
                    }
                    
                    userCompanyRoleRepository.save(userCompanyRole);
                    return ResponseEntity.ok("Reporting Manager user created successfully: " + email + " assigned to company ID: " + companyId + ". Employees can be assigned to this Reporting Manager during employee creation/editing.");
                } else {
                    return ResponseEntity.ok("Reporting Manager user created successfully: " + email + ". Employees can be assigned to this Reporting Manager during employee creation/editing.");
                }
            }
            
            return ResponseEntity.ok("User created successfully: " + email);
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

