package com.application.employee.service.entities;

import com.application.employee.service.deserializer.CustomLocalDateSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "address_verification")
public class AddressVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Column(name = "HOME_ADDRESS")
    private String homeAddress;

    @Column(name = "HOME_ADDRESS_VERIFIED")
    private Boolean homeAddressVerified;

    @JsonSerialize(using = CustomLocalDateSerializer.class)
    @Column(name = "HOME_ADDRESS_VERIFIED_DATE")
    private LocalDate homeAddressVerifiedDate;

    @Column(name = "WORK_ADDRESS")
    private String workAddress;

    @Column(name = "WORK_ADDRESS_VERIFIED")
    private Boolean workAddressVerified;

    @JsonSerialize(using = CustomLocalDateSerializer.class)
    @Column(name = "WORK_ADDRESS_VERIFIED_DATE")
    private LocalDate workAddressVerifiedDate;

    @Column(name = "IS_WORKING")
    private Boolean isWorking;

    @Column(name = "VERIFIED_BY")
    private String verifiedBy;

    @Column(name = "NOTES", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

