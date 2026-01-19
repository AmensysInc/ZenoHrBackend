package com.application.employee.service.entities;

import com.application.employee.service.deserializer.CustomLocalDateSerializer;
import com.application.employee.service.dto.EmployeeDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "employees_details")
public class EmployeeDetails  {
    @Id
    @Column(name = "ID")
    private String employeeDetailsID;
    @Column(name = "FATHERNAME")
    private String fatherName;
    @Column(name = "SSN")
    private String ssn;
    @Column(name = "CURRENT_WORK_LOCATION")
    private String currentWorkLocation;
    @Column(name = "RESIDENTIALADDRESS")
    private String residentialAddress;
    @Column(name = "HOMECOUNTRYADDRESS")
    private String homeCountryAddress;
    @Column(name = "EME_CONTACTDEAILS")
    private String emergencyContactDetails;
    @Column(name = "VISASTATUS")
    private String visaStatus;
    @Column(name = "BACHELORS_DEGREE")
    private String bachelorsDegree;
    @Column(name = "MASTERS_DEGREE")
    private String mastersDegree;
    @Column(name = "BANKNAME")
    private String bankName;
    @Column(name = "ACCOUNTTYPE")
    private String accType;
    @Column(name = "ROUTINGNUMBER")
    private String routingNumber;
    @Column(name = "ACCOUNTNUMBER")
    private String accNumber;
    @Column(name = "MARITALSTATUS")
    private String maritalStatus;
    @Column(name = "ITFILINGSTATE")
    private String itFilingState;
    @Column(name = "NEEDINSURANCE")
    private String needInsurance;
    @Column(name = "STARTDATEWITHAMENSYS")
    @JsonSerialize(using = CustomLocalDateSerializer.class)
    private String startDateWithAmensys;

    @OneToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;
    public EmployeeDetails(EmployeeDTO employeeDTO) {
        this.employeeDetailsID = employeeDTO.getEmployeeID();
        this.fatherName = employeeDTO.getFatherName();
        this.ssn = employeeDTO.getSsn();
        this.currentWorkLocation = employeeDTO.getCurrentWorkLocation();
        this.residentialAddress = employeeDTO.getResidentialAddress();
        this.homeCountryAddress = employeeDTO.getHomeCountryAddress();
        this.emergencyContactDetails = employeeDTO.getEmergencyContactDetails();
        this.visaStatus = employeeDTO.getVisaStatus();
        this.bachelorsDegree = employeeDTO.getBachelorsDegree();
        this.mastersDegree = employeeDTO.getMastersDegree();
        this.bankName = employeeDTO.getBankName();
        this.accType = employeeDTO.getAccType();
        this.routingNumber = employeeDTO.getRoutingNumber();
        this.accNumber = employeeDTO.getAccNumber();
        this.maritalStatus = employeeDTO.getMaritalStatus();
        this.itFilingState  = employeeDTO.getItFilingState();
        this.needInsurance = employeeDTO.getNeedInsurance();
        this.startDateWithAmensys = employeeDTO.getStartDateWithAmensys();
    }
}
