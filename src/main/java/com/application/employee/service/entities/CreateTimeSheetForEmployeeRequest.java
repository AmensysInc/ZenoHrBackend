package com.application.employee.service.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateTimeSheetForEmployeeRequest {

    private TimeSheetMaster timeSheetMaster;

    private TimeSheet timeSheet;
}
