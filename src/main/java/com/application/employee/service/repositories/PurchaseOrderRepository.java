package com.application.employee.service.repositories;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.PurchaseOrder;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder,String> {
    Page<PurchaseOrder> findAll(Specification<PurchaseOrder> spec, Pageable pageable);

    @Query("SELECT po FROM PurchaseOrder po " +
            "WHERE (:companyId IS NULL OR po.employee.company.companyId = :companyId)")
    Page<PurchaseOrder> findByCompanyIdOrAll(@Param("companyId") Long companyId, Pageable pageable);

}
