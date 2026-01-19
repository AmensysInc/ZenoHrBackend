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
import jakarta.persistence.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "visa_details")
public class VisaDetails {
    @Id
    @Column(name = "ID")
    private String visaId;

    @Column(name = "VISA_TYPE")
    private String visaType;

    @JsonSerialize(using = CustomLocalDateSerializer.class)
    @Column(name = "VISA_START_DATE")
    private LocalDate visaStartDate;

    @JsonSerialize(using = CustomLocalDateSerializer.class)
    @Column(name = "VISA_END_DATE")
    private LocalDate visaExpiryDate;

    @JsonSerialize(using = CustomLocalDateSerializer.class)
    @Column(name = "I94_DATE")
    private LocalDate i94Date;

    @Column(name = "LCA_ADDRESS")
    private String lcaAddress;

    @Column(name = "LCA_NUMBER")
    private String lcaNumber;

    @Column(name = "LCA_WAGE")
    private String lcaWage;

    @Column(name = "JOB_TITLE")
    private String jobTitle;

    @Column(name = "I140_STATUS")
    private String i140Status;

    @Column(name = "GC_STATUS")
    private String gcStatus;

    @Column(name = "ATTORNEY")
    private String attorney;

    @Column(name = "RECEIPT")
    private String receipt;

    @Column(name = "RESIDENTIAL_ADDRESS", length = 500)
    private String residentialAddress;

    @Column(name = "COMMENTS", columnDefinition = "TEXT")
    private String comments;

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

}
