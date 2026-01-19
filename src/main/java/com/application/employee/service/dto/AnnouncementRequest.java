package com.application.employee.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AnnouncementRequest {
    private String title;
    private String message;
    private String type;
    private String createdBy;
    private List<Long> employeeIds;
}
