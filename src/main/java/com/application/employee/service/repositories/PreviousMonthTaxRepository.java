package com.application.employee.service.repositories;

import com.application.employee.service.entities.PreviousMonthTax;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PreviousMonthTaxRepository extends JpaRepository<PreviousMonthTax, Long> {
    
    @Query("SELECT p FROM PreviousMonthTax p LEFT JOIN FETCH p.employee WHERE p.employee.employeeID = :employeeId")
    Optional<PreviousMonthTax> findByEmployeeEmployeeID(@Param("employeeId") String employeeId);
}

