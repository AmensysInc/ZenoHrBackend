package com.application.employee.service.repositories;

import com.application.employee.service.entities.TimeSheet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

public interface TimeSheetRepo extends JpaRepository<TimeSheet,Integer> {
    List<TimeSheet> getAllByTimeSheetMasterMasterId(Integer masterId);
    Optional<TimeSheet> findByTimeSheetMasterMasterIdAndDate(Integer masterId, Date date);
}
