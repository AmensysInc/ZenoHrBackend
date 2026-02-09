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

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ytd_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class YTDData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMPLOYEE_ID", nullable = false, unique = true)
    private Employee employee;

    @Column(name = "CURRENT_YEAR", nullable = false)
    private Integer currentYear;

    @Column(name = "YTD_GROSS_PAY", precision = 10, scale = 2)
    private BigDecimal ytdGrossPay = BigDecimal.ZERO;

    @Column(name = "YTD_FEDERAL_TAX", precision = 10, scale = 2)
    private BigDecimal ytdFederalTax = BigDecimal.ZERO;

    @Column(name = "YTD_STATE_TAX", precision = 10, scale = 2)
    private BigDecimal ytdStateTax = BigDecimal.ZERO;

    @Column(name = "YTD_LOCAL_TAX", precision = 10, scale = 2)
    private BigDecimal ytdLocalTax = BigDecimal.ZERO;

    @Column(name = "YTD_SOCIAL_SECURITY", precision = 10, scale = 2)
    private BigDecimal ytdSocialSecurity = BigDecimal.ZERO;

    @Column(name = "YTD_MEDICARE", precision = 10, scale = 2)
    private BigDecimal ytdMedicare = BigDecimal.ZERO;

    @Column(name = "YTD_NET_PAY", precision = 10, scale = 2)
    private BigDecimal ytdNetPay = BigDecimal.ZERO;

    @Column(name = "PAY_PERIODS_COUNT")
    private Integer payPeriodsCount = 0;

    @Column(name = "LAST_PAY_PERIOD")
    private LocalDate lastPayPeriod;

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currentYear == null) {
            currentYear = LocalDate.now().getYear();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

