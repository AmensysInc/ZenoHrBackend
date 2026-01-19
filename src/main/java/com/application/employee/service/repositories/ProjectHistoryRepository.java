package com.application.employee.service.repositories;

import com.application.employee.service.entities.ProjectHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectHistoryRepository extends JpaRepository<ProjectHistory, String> {
    Page<ProjectHistory> findAll(Specification<ProjectHistory> spec, Pageable pageable);

    @Query("SELECT ph FROM ProjectHistory ph " +
            "WHERE (:companyId IS NULL OR ph.employee.company.companyId = :companyId)")
    Page<ProjectHistory> findByCompanyIdOrAll(@Param("companyId") Long companyId, Pageable pageable);
}
