package com.application.employee.service.util;

import com.application.employee.service.user.Role;
import com.application.employee.service.user.User;
import com.application.employee.service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Creates admin user on application startup
 * This will run once when the backend starts
 * 
 * To disable after first run, comment out @Component annotation
 */
@Component
@RequiredArgsConstructor
public class CreateAdminOnStartup implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String email = "rama.k@amensys.com";
        String password = "amenGOTO45@@";
        
        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            System.out.println("✓ Admin user already exists: " + email);
            return;
        }

        // Create admin user
        User adminUser = User.builder()
                .id(UUID.randomUUID().toString())
                .firstname("Rama")
                .lastname("K")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.ADMIN)
                .tempPassword(null)
                .build();

        userRepository.save(adminUser);
        System.out.println("========================================");
        System.out.println("✓ Admin user created successfully!");
        System.out.println("  Email: " + email);
        System.out.println("  Password: " + password);
        System.out.println("  Role: ADMIN");
        System.out.println("========================================");
    }
}

