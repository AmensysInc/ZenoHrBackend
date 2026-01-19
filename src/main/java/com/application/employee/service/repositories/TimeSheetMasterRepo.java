package com.application.employee.service.repositories;

import com.application.employee.service.entities.TimeSheetMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimeSheetMasterRepo extends JpaRepository<TimeSheetMaster,Integer> {
    TimeSheetMaster findByMasterId(Integer masterId);

    TimeSheetMaster findByEmployeeEmployeeIDAndProjectHistoryProjectIdAndMonthAndYear(String employeeId, String projectId, Integer month, Integer year);
    List<TimeSheetMaster> findAllByMonthAndYear(Integer month, Integer year);
}
