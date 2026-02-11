package com.application.employee.service.repositories;

import com.application.employee.service.entities.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {
    
    List<PayrollRecord> findByEmployeeEmployeeID(String employeeId);
    
    @Query("SELECT p FROM PayrollRecord p LEFT JOIN FETCH p.employee WHERE p.employee.employeeID = :employeeId ORDER BY p.payDate DESC")
    List<PayrollRecord> findByEmployeeIdWithEmployee(@Param("employeeId") String employeeId);
    
    @Query("SELECT p FROM PayrollRecord p LEFT JOIN FETCH p.employee ORDER BY p.payDate DESC")
    List<PayrollRecord> findAllWithEmployee();
    
    @Query("SELECT p FROM PayrollRecord p LEFT JOIN FETCH p.employee WHERE p.employee.employeeID = :employeeId ORDER BY p.payDate DESC")
    Optional<PayrollRecord> findLatestByEmployeeId(@Param("employeeId") String employeeId);
    
    @Query("SELECT p FROM PayrollRecord p WHERE p.employee.employeeID = :employeeId AND p.payPeriodEnd < :beforeDate ORDER BY p.payPeriodEnd DESC")
    List<PayrollRecord> findByEmployeeIdAndPayPeriodEndBefore(@Param("employeeId") String employeeId, @Param("beforeDate") LocalDate beforeDate);
    
    @Query("SELECT p FROM PayrollRecord p WHERE p.employee.employeeID = :employeeId AND p.payPeriodEnd < :beforeDate ORDER BY p.payPeriodEnd DESC")
    Optional<PayrollRecord> findLatestByEmployeeIdAndPayPeriodEndBefore(@Param("employeeId") String employeeId, @Param("beforeDate") LocalDate beforeDate);
}

