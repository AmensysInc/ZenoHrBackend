package com.application.employee.service.services;


import com.application.employee.service.entities.TimeSheetMaster;

public interface TimeSheetMasterService {
    public void saveSheets (TimeSheetMaster sheets);

    public TimeSheetMaster getTimeSheetmaster(Integer masterId);
}
