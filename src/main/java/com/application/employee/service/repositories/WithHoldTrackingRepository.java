package com.application.employee.service.repositories;
import com.application.employee.service.dto.EmployeeWithHoldDTO;
import com.application.employee.service.entities.CompanyBalanceProjection;
import com.application.employee.service.entities.PurchaseOrder;
import com.application.employee.service.entities.WithHoldTracking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WithHoldTrackingRepository extends JpaRepository<WithHoldTracking,String> {
    Page<WithHoldTracking> findAll(Specification<WithHoldTracking> spec, Pageable pageable);
//    @Query("SELECT c.companyName AS companyName, SUM(w.balance) AS totalBalance " +
//            "FROM WithHoldTracking w " +
//            "JOIN w.employee e " +
//            "JOIN e.company c " +
//            "GROUP BY c.companyName")
//    List<CompanyBalanceProjection> findCompanyWiseBalance();

    @Query("SELECT c.companyId AS companyId, c.companyName AS companyName, SUM(w.balance) AS totalBalance " +
            "FROM WithHoldTracking w " +
            "JOIN w.employee e " +
            "JOIN e.company c " +
            "GROUP BY c.companyId, c.companyName")
    List<CompanyBalanceProjection> findCompanyWiseBalance();

    @Query("SELECT new com.application.employee.service.dto.EmployeeWithHoldDTO(" +
            "e.firstName, " +
            "e.lastName, " +
            "w.month, " +
            "w.year, " +
            "w.projectName, " +
            "w.actualHours, " +
            "w.actualRate, " +
            "w.actualAmt, " +
            "w.paidHours, " +
            "w.paidRate, " +
            "w.paidAmt, " +
            "w.balance, " +
            "w.type, " +
            "w.status, " +
            "w.billrate) " +
            "FROM WithHoldTracking w " +
            "JOIN w.employee e " +
            "JOIN e.company c " +
            "WHERE c.companyId = :companyId")
    List<EmployeeWithHoldDTO> findEmployeeWithHoldByCompany(@Param("companyId") Integer companyId);

}
