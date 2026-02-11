package com.application.employee.service.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table(name = "paystubs")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Paystub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EMPLOYEE_ID", nullable = false)
    private Employee employee;

    @Column(name = "FILE_NAME", nullable = false)
    private String fileName;

    @Column(name = "FILE_PATH", nullable = false)
    private String filePath;

    @Column(name = "PAY_PERIOD_START", nullable = false)
    private LocalDate payPeriodStart;

    @Column(name = "PAY_PERIOD_END", nullable = false)
    private LocalDate payPeriodEnd;

    @Column(name = "CHECK_DATE")
    private LocalDate checkDate;

    @Column(name = "GROSS_PAY", precision = 10, scale = 2)
    private BigDecimal grossPay;

    @Column(name = "NET_PAY", precision = 10, scale = 2)
    private BigDecimal netPay;

    // YTD (Year-To-Date) values from uploaded paystub
    @Column(name = "YTD_GROSS_PAY", precision = 10, scale = 2)
    private BigDecimal ytdGrossPay;

    @Column(name = "YTD_NET_PAY", precision = 10, scale = 2)
    private BigDecimal ytdNetPay;

    @Column(name = "YTD_FEDERAL_TAX", precision = 10, scale = 2)
    private BigDecimal ytdFederalTax;

    @Column(name = "YTD_STATE_TAX", precision = 10, scale = 2)
    private BigDecimal ytdStateTax;

    @Column(name = "YTD_LOCAL_TAX", precision = 10, scale = 2)
    private BigDecimal ytdLocalTax;

    @Column(name = "YTD_SOCIAL_SECURITY", precision = 10, scale = 2)
    private BigDecimal ytdSocialSecurity;

    @Column(name = "YTD_MEDICARE", precision = 10, scale = 2)
    private BigDecimal ytdMedicare;

    @Column(name = "UPLOADED_AT", nullable = false)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;

    @Column(name = "UPLOADED_BY")
    private String uploadedBy;

    @Column(name = "MONTH")
    private Integer month;

    @Column(name = "YEAR")
    private Integer year;
}

