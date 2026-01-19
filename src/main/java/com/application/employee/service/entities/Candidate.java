package com.application.employee.service.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "candidates")
public class Candidate {
    @Id
    @Column(name = "ID")
    private String candidateID;
    @Column(name = "CANDIDATEFIRSTNAME")
    private String firstName;
    @Column(name = "CANDIDATELASTNAME")
    private String lastName;
    @Column(name = "EMAILADDRESS")
    private String emailAddress;
    @Column(name = "UNIVERSITY")
    private String university;
    @Column(name = "RECRUITERNAME")
    private String recruiterName;
    @Column(name = "COMPANY")
    private String company;
    @Column(name = "SKILLS")
    private String skills;
    @Column(name = "PHONENO")
    private String phoneNo;
    @Column(name = "ORIGINALVISASTATUS")
    private String originalVisaStatus;
    @Column(name = "MARKETING_VISA_STATUS")
    private String marketingVisaStatus;
    @Column(name = "COMMENTS")
    private String comments;
    @Column(name = "CANDIDATESTATUS")
    private String candidateStatus;
    @Column(name = "REFERENCE")
    private String reference;
}
