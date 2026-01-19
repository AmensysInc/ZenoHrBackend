package com.application.employee.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressVerificationRequest {
    private String homeAddress;
    private String workAddress;
    private Boolean isWorking;
    private String notes;
}

