package com.application.employee.service.entities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailRequest {
    private String fromEmail;
    private List<String> toList;
    private List<String> ccList;
    private List<String> bccList;
    private String subject;
    private String body;
    private List<MultipartFile> attachment;
}
