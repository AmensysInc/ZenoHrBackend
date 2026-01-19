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
public class AddressVerificationDTO {
    private Long id;
    private String employeeId;
    private String employeeName;
    private String employeeEmail;
    private String homeAddress;
    private Boolean homeAddressVerified;
    private LocalDate homeAddressVerifiedDate;
    private String workAddress;
    private Boolean workAddressVerified;
    private LocalDate workAddressVerifiedDate;
    private Boolean isWorking;
    private String verifiedBy;
    private String notes;
}

