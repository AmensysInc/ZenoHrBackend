package com.application.employee.service.util;

import com.application.employee.service.entities.Companies;
import com.application.employee.service.entities.UserCompanyRole;
import com.application.employee.service.repositories.CompaniesRepository;
import com.application.employee.service.repositories.UserCompanyRoleRepository;
import com.application.employee.service.repositories.UserRepository;
import com.application.employee.service.user.Role;
import com.application.employee.service.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Creates admin user and default company on application startup
 * This will run once when the backend starts
 * 
 * To disable after first run, comment out @Component annotation
 */
@Component
@RequiredArgsConstructor
public class CreateAdminOnStartup implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CompaniesRepository companiesRepository;
    private final UserCompanyRoleRepository userCompanyRoleRepository;

    @Override
    public void run(String... args) throws Exception {
        String email = "rama.k@amensys.com";
        String password = "amenGOTO45@@";
        
        // Get or create admin user
        User adminUser = userRepository.findByEmail(email).orElse(null);
        
        if (adminUser == null) {
            // Create Super Admin user (SADMIN - no company assignment)
            adminUser = User.builder()
                    .id(UUID.randomUUID().toString())
                    .firstname("Rama")
                    .lastname("K")
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .role(Role.SADMIN)  // SADMIN - Super Admin
                    .tempPassword(null)
                    .build();

            userRepository.save(adminUser);
            System.out.println("========================================");
            System.out.println("✓ Super Admin user created successfully!");
            System.out.println("  Email: " + email);
            System.out.println("  Password: " + password);
            System.out.println("  Role: SADMIN (Super Admin - no company assignment)");
            System.out.println("========================================");
        } else {
            // Update existing user to SADMIN if not already
            if (adminUser.getRole() != Role.SADMIN) {
                adminUser.setRole(Role.SADMIN);
                userRepository.save(adminUser);
                System.out.println("✓ Updated existing user to SADMIN: " + email);
            } else {
                System.out.println("✓ Super Admin user already exists: " + email);
            }
            
            // Remove any company assignments for SADMIN
            List<UserCompanyRole> existingRoles = userCompanyRoleRepository.findByUserId(adminUser.getId());
            if (!existingRoles.isEmpty()) {
                System.out.println("✓ Removing company assignments for SADMIN (SADMIN should not have company)");
                for (UserCompanyRole role : existingRoles) {
                    userCompanyRoleRepository.delete(role);
                }
            }
        }
        
        // SADMIN should NOT have any company assignment
        // No default company creation or assignment for SADMIN
    }
}

