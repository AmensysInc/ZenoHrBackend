package com.application.employee.service.repositories;

import com.application.employee.service.entities.VisaDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisaDetailsRepository extends JpaRepository<VisaDetails, String> {
    Page<VisaDetails> findAll(Specification<VisaDetails> spec, Pageable pageable);

    @Query("SELECT v FROM VisaDetails v " +
            "WHERE (:companyId IS NULL OR v.employee.company.companyId = :companyId)")
    Page<VisaDetails> findByCompanyIdOrAll(@Param("companyId") Long companyId, Pageable pageable);

}
