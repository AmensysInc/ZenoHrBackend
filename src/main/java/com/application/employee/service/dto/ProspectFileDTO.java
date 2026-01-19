package com.application.employee.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProspectFileDTO {
    private String fileName;
    private LocalDateTime uploadTime;
    private String uploadedBy;
    private String employeeId;
    private String firstName;
    private String lastName;
}
