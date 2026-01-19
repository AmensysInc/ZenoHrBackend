package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.TimeSheetMaster;
import com.application.employee.service.exceptions.ResourceNotFoundException;
import com.application.employee.service.repositories.TimeSheetMasterRepo;
import com.application.employee.service.services.TimeSheetMasterService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TimeSheetMasterServiceImpl implements TimeSheetMasterService {
    private TimeSheetMasterRepo timeSheetMasterRepo;
    @Override
    public void saveSheets(TimeSheetMaster sheets) {
        timeSheetMasterRepo.save(sheets);
    }

    @Override
    public TimeSheetMaster getTimeSheetmaster(Integer masterId) {
        return timeSheetMasterRepo.findByMasterId(masterId);
    }
}
