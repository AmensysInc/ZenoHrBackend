package com.application.employee.service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Mail Configuration - Only enabled if explicitly configured
 * 
 * NOTE: If you're using SendGrid for all emails, you can disable this entirely
 * by not setting spring.mail.enabled=true in your configuration.
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Bean
    @ConditionalOnProperty(name = "spring.mail.enabled", havingValue = "true", matchIfMissing = false)
    public JavaMailSender javaMailSender() {
        // Validate configuration
        if (mailUsername == null || mailUsername.trim().isEmpty()) {
            throw new IllegalStateException(
                "Spring mail is enabled but mail.username is not configured. " +
                "Set spring.mail.username in your configuration or disable mail by " +
                "not setting spring.mail.enabled=true"
            );
        }
        
        if (mailPassword == null || mailPassword.trim().isEmpty()) {
            throw new IllegalStateException(
                "Spring mail is enabled but mail.password is not configured. " +
                "Set spring.mail.password in your configuration or disable mail by " +
                "not setting spring.mail.enabled=true"
            );
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(mailHost);
        mailSender.setPort(mailPort);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        
        // Only enable debug in development
        String debug = System.getProperty("mail.debug", "false");
        props.put("mail.debug", debug);
        
        return mailSender;
    }
}
