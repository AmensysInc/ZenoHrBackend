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

