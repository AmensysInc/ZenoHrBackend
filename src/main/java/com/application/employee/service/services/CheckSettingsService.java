package com.application.employee.service.services;

import com.application.employee.service.entities.CheckSettings;
import com.application.employee.service.entities.Companies;

import java.util.List;

public interface CheckSettingsService {
    
    CheckSettings getOrCreateCheckSettings(Integer companyId);
    
    CheckSettings updateCheckNumber(Integer companyId, Long checkNumber);
    
    Long getNextCheckNumber(Integer companyId);
    
    List<CheckSettings> getAllCheckSettings();
    
    CheckSettings getCheckSettingsByCompanyId(Integer companyId);
}

