package com.application.employee.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "profit_loss")
public class ProfitAndLoss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    private String type;

    private Integer hours;

    private Double rate;

    private Double amount;

    private Double otherAmount;

    private Double totalAmount;

    private String status;

    @Column(length = 2000)
    private String notes;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    private Employee employee;

    @JsonProperty("employeeId")
    public String getEmployeeId() {
        return employee != null ? employee.getEmployeeID() : null;
    }
}
