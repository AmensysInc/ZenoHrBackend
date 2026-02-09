package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.CheckSettings;
import com.application.employee.service.entities.Companies;
import com.application.employee.service.repositories.CheckSettingsRepository;
import com.application.employee.service.repositories.CompaniesRepository;
import com.application.employee.service.services.CheckSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CheckSettingsServiceImpl implements CheckSettingsService {

    @Autowired
    private CheckSettingsRepository checkSettingsRepository;

    @Autowired
    private CompaniesRepository companiesRepository;

    @Override
    public CheckSettings getOrCreateCheckSettings(Integer companyId) {
        Optional<CheckSettings> existing = checkSettingsRepository.findByCompanyId(companyId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // Create new check settings for the company
        Companies company = companiesRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));

        CheckSettings checkSettings = new CheckSettings();
        checkSettings.setCompany(company);
        checkSettings.setCurrentCheckNumber(1L);
        return checkSettingsRepository.save(checkSettings);
    }

    @Override
    @Transactional
    public CheckSettings updateCheckNumber(Integer companyId, Long checkNumber) {
        CheckSettings checkSettings = getOrCreateCheckSettings(companyId);
        checkSettings.setCurrentCheckNumber(checkNumber);
        return checkSettingsRepository.save(checkSettings);
    }

    @Override
    @Transactional
    public Long getNextCheckNumber(Integer companyId) {
        CheckSettings checkSettings = getOrCreateCheckSettings(companyId);
        Long nextNumber = checkSettings.getCurrentCheckNumber();
        checkSettings.setCurrentCheckNumber(nextNumber + 1);
        checkSettingsRepository.save(checkSettings);
        return nextNumber;
    }

    @Override
    public List<CheckSettings> getAllCheckSettings() {
        return checkSettingsRepository.findAllWithCompany();
    }

    @Override
    public CheckSettings getCheckSettingsByCompanyId(Integer companyId) {
        return getOrCreateCheckSettings(companyId);
    }
}

