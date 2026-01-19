package com.application.employee.service.repositories;

import com.application.employee.service.entities.TimeSheetFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimeSheetFileRepo extends JpaRepository<TimeSheetFile, Long> {
    List<TimeSheetFile> findAllByTimeSheetMasterMasterId(Integer masterId);
    List<TimeSheetFile> findByFileNameAndTimeSheetMasterEmployeeEmployeeID(String fileName, String employeeId);
}
