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
@Table(name = "previous_month_taxes")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PreviousMonthTax {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMPLOYEE_ID", nullable = false)
    private Employee employee;

    @Column(name = "PERIOD_START_DATE", nullable = false)
    private LocalDate periodStartDate;

    @Column(name = "PERIOD_END_DATE", nullable = false)
    private LocalDate periodEndDate;

    @Column(name = "FEDERAL_TAX_WITHHELD", precision = 10, scale = 2)
    private BigDecimal federalTaxWithheld;

    @Column(name = "STATE_TAX_WITHHELD", precision = 10, scale = 2)
    private BigDecimal stateTaxWithheld;

    @Column(name = "STATE_TAX_NAME")
    private String stateTaxName;

    @Column(name = "LOCAL_TAX_WITHHELD", precision = 10, scale = 2)
    private BigDecimal localTaxWithheld;

    @Column(name = "SOCIAL_SECURITY_WITHHELD", precision = 10, scale = 2)
    private BigDecimal socialSecurityWithheld;

    @Column(name = "MEDICARE_WITHHELD", precision = 10, scale = 2)
    private BigDecimal medicareWithheld;

    @Column(name = "TOTAL_GROSS_PAY", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalGrossPay;

    @Column(name = "TOTAL_NET_PAY", precision = 10, scale = 2, nullable = false)
    private BigDecimal totalNetPay;

    // H1B specific fields
    @Column(name = "H1B_WAGE", precision = 10, scale = 2)
    private BigDecimal h1bWage;

    @Column(name = "H1B_PREVAILING_WAGE", precision = 10, scale = 2)
    private BigDecimal h1bPrevailingWage;

    // Additional fields from PDF stored as JSON
    @Column(name = "ADDITIONAL_FIELDS", columnDefinition = "TEXT")
    private String additionalFieldsJson;

    // PDF file path
    @Column(name = "PDF_FILE_PATH")
    private String pdfFilePath;

    @Column(name = "PDF_FILE_NAME")
    private String pdfFileName;

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

