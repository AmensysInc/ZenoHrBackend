package com.application.employee.service.repositories;

import com.application.employee.service.entities.PreviousMonthTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreviousMonthTaxRepository extends JpaRepository<PreviousMonthTax, Long> {
    
    @Query("SELECT p FROM PreviousMonthTax p LEFT JOIN FETCH p.employee WHERE p.employee.employeeID = :employeeId ORDER BY p.periodEndDate DESC")
    Optional<PreviousMonthTax> findByEmployeeEmployeeID(@Param("employeeId") String employeeId);
    
    @Query("SELECT p FROM PreviousMonthTax p LEFT JOIN FETCH p.employee WHERE p.employee.employeeID = :employeeId ORDER BY p.periodEndDate DESC")
    java.util.List<PreviousMonthTax> findAllByEmployeeEmployeeIDOrderByPeriodEndDateDesc(@Param("employeeId") String employeeId);
    
    @Query("SELECT p FROM PreviousMonthTax p LEFT JOIN FETCH p.employee ORDER BY p.createdAt DESC")
    java.util.List<PreviousMonthTax> findAllWithEmployee();
}

