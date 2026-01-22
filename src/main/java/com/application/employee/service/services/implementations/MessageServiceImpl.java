package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Message;
import com.application.employee.service.repositories.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MessageServiceImpl {

    @Autowired
    private MessageRepository repository;

    public Message save(Message message) {
        return repository.save(message);
    }

    public List<Message> getAll() {
        return repository.findAll();
    }

    public Optional<Message> getById(Long id) {
        return repository.findById(id);
    }

    public Message update(Long id, Message newData) {
        return repository.findById(id).map(msg -> {
            msg.setName(newData.getName());
            msg.setSubject(newData.getSubject());
            msg.setBody(newData.getBody());
            msg.setDescription(newData.getDescription());
            msg.setCategory(newData.getCategory());
            msg.setIsActive(newData.getIsActive());
            return repository.save(msg);
        }).orElseThrow(() -> new RuntimeException("Message not found"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Optional<Message> getByCategory(String category) {
        return repository.findByCategoryAndIsActive(category, true);
    }

    public void initializeDefaultTemplates() {
        // Define system email template categories
        String[] systemCategories = {
            "FORGOT_PASSWORD",
            "LOGIN_DETAILS",
            "CHANGE_PASSWORD",
            "WELCOME"
        };

        for (String category : systemCategories) {
            // Check if template already exists for this category
            Optional<Message> existing = repository.findByCategoryAndIsActive(category, true);
            if (existing.isEmpty()) {
                // Create default template
                Message template = new Message();
                template.setName(getDefaultTemplateName(category));
                template.setDescription(getDefaultTemplateDescription(category));
                template.setSubject(getDefaultTemplateSubject(category));
                template.setBody(getDefaultTemplateBody(category));
                template.setCategory(category);
                template.setIsActive(true);
                repository.save(template);
            }
        }
    }

    private String getDefaultTemplateName(String category) {
        return switch (category.toUpperCase()) {
            case "FORGOT_PASSWORD" -> "Forgot Password";
            case "LOGIN_DETAILS" -> "Login Details";
            case "CHANGE_PASSWORD" -> "Change Password";
            case "WELCOME" -> "Welcome Email";
            default -> "System Template";
        };
    }

    private String getDefaultTemplateDescription(String category) {
        return switch (category.toUpperCase()) {
            case "FORGOT_PASSWORD" -> "Email template sent when user requests password reset";
            case "LOGIN_DETAILS" -> "Email template sent when new user login details are created";
            case "CHANGE_PASSWORD" -> "Email template sent when password is changed";
            case "WELCOME" -> "Welcome email template for new users";
            default -> "System email template";
        };
    }

    private String getDefaultTemplateSubject(String category) {
        return switch (category.toUpperCase()) {
            case "FORGOT_PASSWORD" -> "Password Reset Request";
            case "LOGIN_DETAILS" -> "Your Login Credentials";
            case "CHANGE_PASSWORD" -> "Password Change Confirmation";
            case "WELCOME" -> "Welcome to HR Portal";
            default -> "Notification from HR Portal";
        };
    }

    private String getDefaultTemplateBody(String category) {
        return switch (category.toUpperCase()) {
            case "FORGOT_PASSWORD" -> "Hello,\n\n" +
                    "You have requested to reset your password. Your temporary password is: {temp_password}\n\n" +
                    "Please login and change your password immediately.\n\n" +
                    "If you did not request this, please contact support.\n\n" +
                    "Regards,\nTeam HR";
            case "LOGIN_DETAILS" -> "Hello,\n\n" +
                    "Your login credentials have been created:\n" +
                    "Email: {email_address}\n" +
                    "Temporary Password: {temp_password}\n\n" +
                    "Please login and change your password immediately.\n\n" +
                    "Regards,\nTeam HR";
            case "CHANGE_PASSWORD" -> "Hello,\n\n" +
                    "Your password has been changed successfully.\n\n" +
                    "If you did not make this change, please contact support immediately.\n\n" +
                    "Regards,\nTeam HR";
            case "WELCOME" -> "Hello,\n\n" +
                    "Welcome to our HR Portal!\n\n" +
                    "We are excited to have you on board.\n\n" +
                    "Regards,\nTeam HR";
            default -> "Hello,\n\nThis is a system notification.\n\nRegards,\nTeam HR";
        };
    }
}

