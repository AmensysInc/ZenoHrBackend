package com.application.employee.service.entities;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Blob;
import java.time.LocalDate;
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "withhold_tracking")
public class WithHoldTracking {
    @Id
    @Column(name = "ID")
    private String trackingId;

    @Column(name = "MONTH")
    private String month;

    @Column(name = "YEAR")
    private String year;

    @Lob
    @Column(name = "EXCEL_DATA", columnDefinition = "longtext")
    private String excelData;

    @Column(name = "PROJECT_NAME")
    private String projectName;

    @Column(name = "ACTUAL_HOURS")
    private BigDecimal actualHours;

    @Column(name = "ACTUAL_RATE")
    private BigDecimal actualRate;

    @Column(name = "ACTUAL_AMT")
    private BigDecimal actualAmt;

    @Column(name = "PAID_HOURS")
    private BigDecimal paidHours;

    @Column(name = "PAID_RATE")
    private BigDecimal paidRate;

    @Column(name = "PAID_AMT")
    private BigDecimal paidAmt;

    @Column(name = "BALANCE")
    private BigDecimal balance;

    @Column(name = "TYPE")
    private String type;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "BILLRATE")
    private String billrate;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "employee_id")
    private Employee employee;
}
