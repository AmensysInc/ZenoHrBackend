package com.application.employee.service.dto;

import com.application.employee.service.user.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeDTO {

    private String employeeID;
    private String firstName;
    private String middleName;
    private String lastName;
    private String emailID;
    private String clgOfGrad;
    private String phoneNo;
    private LocalDate dob;
    private String onBench;
    private Role securityGroup;
    private Integer companyId;
    private String company;
    private String password;
    private String employeeDetailsID;
    private String fatherName;
    private String ssn;
    private String currentWorkLocation;
    private String workingLocation;
    private String homeLocation;
    private String residentialAddress;
    private String homeCountryAddress;
    private String emergencyContactDetails;
    private String visaStatus;
    private String bachelorsDegree;
    private String mastersDegree;
    private String bankName;
    private String accType;
    private String routingNumber;
    private String accNumber;
    private String maritalStatus;
    private String itFilingState;
    private String needInsurance;
    private String startDateWithAmensys;
    private String reportingManagerId;
}
