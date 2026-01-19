package com.application.employee.service.entities;

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
@Table(name = "emails")
public class BulkMail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "FIRSTNAME")
    private String firstName;
    @Column(name = "LASTNAME")
    private String lastName;
    @Column(name = "PHONE")
    private String phoneNumber;
    @Column(name = "EMAIL", unique = true)
    private String email;
    @Column(name = "RECRUITER_ID")
    private String recruiterId;
    @Column(name = "LINKEDIN_LINK")
    private String linkedinLink;
}
