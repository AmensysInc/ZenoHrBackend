package com.application.employee.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payroll_records")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMPLOYEE_ID", nullable = false)
    private Employee employee;

    @Column(name = "PAY_PERIOD_START", nullable = false)
    private LocalDate payPeriodStart;

    @Column(name = "PAY_PERIOD_END", nullable = false)
    private LocalDate payPeriodEnd;

    @Column(name = "PAY_DATE", nullable = false)
    private LocalDate payDate;

    @Column(name = "GROSS_PAY", precision = 10, scale = 2, nullable = false)
    private BigDecimal grossPay;

    // Tax deductions
    @Column(name = "FEDERAL_TAX", precision = 10, scale = 2)
    private BigDecimal federalTax;

    @Column(name = "STATE_TAX", precision = 10, scale = 2)
    private BigDecimal stateTax;

    @Column(name = "STATE_TAX_NAME")
    private String stateTaxName;

    @Column(name = "LOCAL_TAX", precision = 10, scale = 2)
    private BigDecimal localTax;

    @Column(name = "SOCIAL_SECURITY", precision = 10, scale = 2)
    private BigDecimal socialSecurity;

    @Column(name = "MEDICARE", precision = 10, scale = 2)
    private BigDecimal medicare;

    @Column(name = "ADDITIONAL_MEDICARE", precision = 10, scale = 2)
    private BigDecimal additionalMedicare;

    // Other deductions
    @Column(name = "HEALTH_INSURANCE", precision = 10, scale = 2)
    private BigDecimal healthInsurance;

    @Column(name = "RETIREMENT_401K", precision = 10, scale = 2)
    private BigDecimal retirement401k;

    @Column(name = "OTHER_DEDUCTIONS", precision = 10, scale = 2)
    private BigDecimal otherDeductions;

    // Custom deductions stored as JSON
    @Column(name = "CUSTOM_DEDUCTIONS", columnDefinition = "TEXT")
    private String customDeductionsJson;

    @Column(name = "TOTAL_DEDUCTIONS", precision = 10, scale = 2)
    private BigDecimal totalDeductions;

    @Column(name = "NET_PAY", precision = 10, scale = 2, nullable = false)
    private BigDecimal netPay;

    // YTD values at time of this payroll
    @Column(name = "YTD_GROSS_PAY", precision = 10, scale = 2)
    private BigDecimal ytdGrossPay;

    @Column(name = "YTD_NET_PAY", precision = 10, scale = 2)
    private BigDecimal ytdNetPay;

    @Column(name = "STATUS")
    private String status; // 'draft', 'processed', 'paid'

    @Column(name = "PAYSTUB_GENERATED")
    private Boolean paystubGenerated = false;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

