package com.application.employee.service.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Date;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "TIME_SHEET")
public class TimeSheet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SHEET_ID")
    private Integer sheetId;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "MASTER_ID")
    private TimeSheetMaster timeSheetMaster;

    @Column(name = "REGULAR_HOURS")
    private Double regularHours;

    @Column(name = "OVERTIME_HOURS")
    private Double overTimeHours;

    @Column(name = "DATE")
    private Date date;

    @Column(name = "STATUS")
    private String status;

    @Column(name = "NOTES")
    private String notes;

    @Transient
    private String empId;

    @Transient
    private String projectId;
}
