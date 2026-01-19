package com.application.employee.service.util;

import com.application.employee.service.user.Role;
import com.application.employee.service.user.User;
import com.application.employee.service.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Utility class to create admin user
 * Run this once to create the admin user
 * 
 * To use: Uncomment the @Component annotation and run the application
 * Then comment it back to prevent re-creation
 */
// @Component
public class CreateAdminUser implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        String email = "rama.k@amensys.com";
        String password = "amenGOTO45@@";
        
        // Check if user already exists
        if (userRepository.findByEmail(email).isPresent()) {
            System.out.println("Admin user already exists: " + email);
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
        System.out.println("Admin user created successfully: " + email);
        System.out.println("Password: " + password);
    }
}

