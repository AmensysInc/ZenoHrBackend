package com.application.employee.service.repositories;

import com.application.employee.service.entities.CheckSettings;
import com.application.employee.service.entities.Companies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CheckSettingsRepository extends JpaRepository<CheckSettings, Long> {
    
    Optional<CheckSettings> findByCompany(Companies company);
    
    Optional<CheckSettings> findByCompanyId(Integer companyId);
    
    @Query("SELECT c FROM CheckSettings c LEFT JOIN FETCH c.company")
    List<CheckSettings> findAllWithCompany();
}

