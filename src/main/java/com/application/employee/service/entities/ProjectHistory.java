package com.application.employee.service.entities;

import com.application.employee.service.deserializer.CustomLocalDateSerializer;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@Table(name = "project_history")
public class ProjectHistory {
    @Id
    @Column(name = "ID")
    private String projectId;
    @Column(name = "SUBVENDOR_ONE")
    private String subVendorOne;
    @Column(name = "SUBVENDOR_TWO")
    private String subVendorTwo;
    @Column(name = "PROJECT_ADDRESS")
    private String projectAddress;
    @JsonSerialize(using = CustomLocalDateSerializer.class)
    @Column(name = "PROJECT_START_DATE")
    private LocalDate projectStartDate;
    @JsonSerialize(using = CustomLocalDateSerializer.class)
    @Column(name = "PROJECT_END_DATE")
    private LocalDate projectEndDate;
    @Column(name = "PROJECT_STATUS")
    private String projectStatus;
    @Column(name = "NET_TERMS")
    private String netTerms;

    @Column(name = "RECRUITER_NAME")
    private String recruiterName;
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @JsonProperty("employeeName")
    public String getEmployeeName() {
        return employee != null
                ? employee.getFirstName() + " " + employee.getLastName()
                : null;
    }

    @JsonProperty("employeeEmail")
    public String getEmployeeEmail() {
        return employee != null ? employee.getEmailID() : null;
    }

    @Column(name = "CREATED_AT", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
