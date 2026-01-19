package com.application.employee.service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AddressVerificationUpdateRequest {
    private String homeAddress;
    private Boolean homeAddressVerified;
    private String workAddress;
    private Boolean workAddressVerified;
    private Boolean isWorking;
    private String verifiedBy;
    private String notes;
}

