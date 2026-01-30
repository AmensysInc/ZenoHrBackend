package com.application.employee.service.entities;
import com.application.employee.service.deserializer.CustomLocalDateSerializer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.application.employee.service.dto.EmployeeDTO;
import com.application.employee.service.user.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@Table(name = "employees")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Employee {

    @Id
    @Column(name = "ID")
    private String employeeID;

    @Column(name = "FIRSTNAME")
    private String firstName;

    @Column(name = "MIDDLENAME")
    private String middleName;

    @Column(name = "LASTNAME")
    private String lastName;

    @Column(name = "EMAILID", unique = true)
    private String emailID;

    @Column(name = "COLLEGE_OF_GRADUATION")
    private String clgOfGrad;

    @Column(name = "PHONE_NO")
    private String phoneNo;

    @Column(name = "dob")
    @JsonSerialize(using = CustomLocalDateSerializer.class)
    private LocalDate dob;

    @Column(name = "ON_BENCH")
    private String onBench;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COMPANY_ID", referencedColumnName = "COMPANY_ID")
    private Companies company;

    @Column(name = "COMPANY_ID", insertable = false, updatable = false)
    private Long CompanyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE")
    private Role securityGroup;

    @Column(name = "PASSWORD")
    private String password;

    @Column(name = "REPORTING_MANAGER_ID")
    private String reportingManagerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REPORTING_MANAGER_ID", insertable = false, updatable = false)
    @JsonIgnore
    private com.application.employee.service.user.User reportingManager;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<PurchaseOrder> employeePurchaseOrder;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<WithHoldTracking> employeeWithHoldTracking;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<ProjectHistory> employeeProjectHistory;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<VisaDetails> employeeVisaDetails;

    @OneToOne(mappedBy = "employee", cascade = CascadeType.ALL)
    private EmployeeDetails employeeDetails;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private List<TimeSheetMaster> timeSheetMasters;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<ProfitAndLoss> profitAndLosses;

    public Employee(EmployeeDTO employeeDTO) {

        this.employeeID = employeeDTO.getEmployeeID();
        this.firstName = employeeDTO.getFirstName();
        this.middleName = employeeDTO.getMiddleName();
        this.lastName = employeeDTO.getLastName();
        this.emailID = employeeDTO.getEmailID();
        this.clgOfGrad = employeeDTO.getClgOfGrad();
        this.phoneNo = employeeDTO.getPhoneNo();
        this.dob = employeeDTO.getDob();
        this.onBench = employeeDTO.getOnBench();
        this.securityGroup = employeeDTO.getSecurityGroup();
        if (employeeDTO.getCompanyId() != null) {
            this.company = new Companies();
            this.company.setCompanyId(employeeDTO.getCompanyId());
        }
        this.password = employeeDTO.getPassword();
        this.reportingManagerId = employeeDTO.getReportingManagerId();
    }

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}