package com.application.employee.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "COMPANIES")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Companies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "COMPANY_ID")
    private Integer companyId;

    @Column(name = "COMPANY_NAME")
    private String companyName;

    @Column(name = "COMPANY_CODE")
    private String companyCode;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "SECONDARY_EMAIL")
    private String secondaryEmail;

    @Column(name = "ADDRESS_LINE1")
    private String addressLine1;

    @Column(name = "ADDRESS_LINE2")
    private String addressLine2;

    @Column(name = "CITY")
    private String city;

    @Column(name = "STATE")
    private String state;

    @Column(name = "ZIP_CODE")
    private String zipCode;

    @Column(name = "PHONE_NO")
    private String phoneNo;

    @Column(name = "DOCUMENT_NAME")
    private String documentName;

    @Column(name = "DOCUMENT_PATH")
    private String documentPath;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Employee> employees;

    @OneToMany(mappedBy = "company", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<UserCompanyRole> userCompanyRoles;
}
