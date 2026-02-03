package com.application.employee.service.auth;

import com.application.employee.service.config.JwtService;
import com.application.employee.service.config.SendGridEmail;
import com.application.employee.service.entities.Message;
import com.application.employee.service.repositories.EmployeeRespository;
import com.application.employee.service.repositories.MessageRepository;
import com.application.employee.service.user.User;
import com.application.employee.service.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationProvider authenticationProvider;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private SendGridEmail sendGridEmail;
    @Autowired
    private EmployeeRespository employeeRepository;

    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
                .id(UUID.randomUUID().toString())
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .role(request.getRole())
                .build();

        String tempPassword = UUID.randomUUID().toString();
        user.setTempPassword(passwordEncoder.encode(tempPassword));
        sendTemporaryPasswordEmail(user.getEmail(), tempPassword);

        repository.save(user);
        var jwtToken = jwtService.generateToken(user);
        return AuthenticationResponse.builder().build();
    }

    public ResponseEntity<String> reset(String email, String category) {
        try {
            // 1️⃣ Validate User
            Optional<User> userOpt = repository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found with given emailID");
            }

            User user = userOpt.get();
            String tempPassword = null;

            // 2️⃣ Generate Temp Password for categories that need it
            if ("FORGOT_PASSWORD".equalsIgnoreCase(category) || "CHANGE_PASSWORD".equalsIgnoreCase(category) || "LOGIN_DETAILS".equalsIgnoreCase(category)) {
                tempPassword = generateTempPassword();
                user.setTempPassword(passwordEncoder.encode(tempPassword));
                System.out.println("Generated Temp Password: " + tempPassword);
            }

            // 3️⃣ Prepare placeholders (dynamic replacement for templates)
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("email_address", email);
            placeholders.put("website_link", "https://zenopayhr.com/quick-hrms-ui/login"); // Add login page link
            if (tempPassword != null) {
                placeholders.put("temp_password", tempPassword);
            }

            // 4️⃣ Send Email via template
            sendEmailUsingTemplate(email, category, placeholders);

            // 5️⃣ Save user only if password was generated
            if (tempPassword != null) {
                repository.save(user);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body("Email sent successfully for category: " + category);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Email sending failed for category: " + category + " | Error: " + e.getMessage());
        }
    }


    /**
     * Generates a secure random alphanumeric temporary password (10 characters long).
     */
    private String generateTempPassword() {
        final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        final int PASSWORD_LENGTH = 10;
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(PASSWORD_LENGTH);
        for (int i = 0; i < PASSWORD_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    public void sendEmailUsingTemplate(String toEmail, String category, Map<String, String> placeholders) throws Exception {
        // 1️⃣ Try fetching template for this category
        Optional<Message> templateOpt = messageRepository.findByCategoryAndIsActive(category, true);

        String subject;
        String body;

        if (templateOpt.isPresent()) {
            Message template = templateOpt.get();
            subject = replacePlaceholders(template.getSubject(), placeholders);
            body = replacePlaceholders(template.getBody(), placeholders);
        } else {
            // Fallback generic template
            subject = switch (category.toUpperCase()) {
                case "FORGOT_PASSWORD" -> "Password Reset";
                case "CHANGE_PASSWORD" -> "Password Change Confirmation";
                default -> "Notification from HR Portal";
            };

            body = "Hello,\n\n";

            if (placeholders.containsKey("temp_password")) {
                body += "Your temporary password is: " + placeholders.get("temp_password") + "\n\n";
            }

            body += "Regards,\nTeam HR";
        }

        // 2️⃣ Determine the 'from' email based on category
        String fromEmail;
        String categoryUpper = category != null ? category.toUpperCase() : "";
        
        if ("FORGOT_PASSWORD".equals(categoryUpper)) {
            // Use support@zenopayhr.com for password reset emails
            fromEmail = "support@zenopayhr.com";
        } else {
            // For all other emails (LOGIN_DETAILS, CHANGE_PASSWORD, etc.), use company email
            fromEmail = employeeRepository.findCompanyEmailByEmployeeEmail(toEmail);
            if (!StringUtils.hasText(fromEmail)) {
                // Fallback: try to get from employee directly if repository method returns null
                Optional<com.application.employee.service.entities.Employee> employeeOpt = 
                    employeeRepository.findByEmailID(toEmail);
                if (employeeOpt.isPresent() && employeeOpt.get().getCompany() != null 
                    && StringUtils.hasText(employeeOpt.get().getCompany().getEmail())) {
                    fromEmail = employeeOpt.get().getCompany().getEmail();
                } else {
                    // Final fallback - should not happen if companies are properly configured
                    fromEmail = "support@zenopayhr.com";
                }
            }
        }

        // 3️⃣ Send Email using SendGrid
        sendGridEmail.sendEmails(
                fromEmail,
                List.of(toEmail),
                null,
                null,
                subject,
                body,
                List.of()
        );

        int statusCode = sendGridEmail.getLastStatusCode();
        if (statusCode != 202) {
            throw new Exception("SendGrid Error: " + sendGridEmail.getLastErrorMessage());
        }

        System.out.println("[Email Sent] Category: " + category + ", From: " + fromEmail + ", To: " + toEmail);
    }


    private String replacePlaceholders(String text, Map<String, String> placeholders) {
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            // Replace both {{placeholder}} and {placeholder} formats
            text = text.replace("{{" + entry.getKey() + "}}", entry.getValue());
            text = text.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return text;
    }


    public ResponseEntity<String> updatePassword(String userId, String password) {
        var user = repository.findById(userId);
        user.setPassword(passwordEncoder.encode(password));
        user.setTempPassword(null);
        repository.save(user);
        return  ResponseEntity.status(HttpStatus.CREATED).body("Password updated");
    }


    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean hasTempPassword = StringUtils.hasText(user.getTempPassword());
        boolean passwordMatches = false;

        // Check if user is using temporary password
        if (hasTempPassword) {
            // Verify the provided password against the encoded temp password
            if (passwordEncoder.matches(request.getPassword(), user.getTempPassword())) {
                passwordMatches = true;
                // Set the temp password as the main password so user can continue using it
                user.setPassword(user.getTempPassword());
                user.setTempPassword(null); // Clear temp password after first successful login
                repository.save(user);
            }
        }

        // If not using temp password, check regular password
        if (!passwordMatches) {
            if (user.getPassword().startsWith("$2a$")) {
                // Password is bcrypt encoded, use authentication provider
                authenticationProvider.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                request.getEmail(),
                                request.getPassword()
                        )
                );
            } else {
                // Plain text password (legacy)
                if (!user.getPassword().equals(request.getPassword())) {
                    throw new BadCredentialsException("Invalid password");
                }
            }
        }

        var jwtToken = jwtService.generateToken(user);

        return AuthenticationResponse.builder()
                .accessToken(jwtToken)
                .id(user.getId())
                .firstName(user.getFirstname())
                .lastName(user.getLastname())
                .tempPassword(hasTempPassword && passwordMatches)
                .role(user.getRole())
                .build();
    }

    public void sendTemporaryPasswordEmail(String toEmail, String tempPassword) {
        // Use the same template system as other emails for consistency
        // This is for user registration - will use company email (LOGIN_DETAILS category)
        String category = "LOGIN_DETAILS";

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("email_address", toEmail);
        placeholders.put("emailID", toEmail); // Support both placeholder names
        placeholders.put("temp_password", tempPassword);
        placeholders.put("password", tempPassword); // Support both placeholder names

        try {
            // Use the template-based email sending which handles company email lookup
            sendEmailUsingTemplate(toEmail, category, placeholders);
        } catch (Exception e) {
            // Log error but don't throw - registration should still succeed even if email fails
            System.err.println("Failed to send temporary password email to " + toEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}

