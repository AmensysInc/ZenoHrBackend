package com.application.employee.service.repositories;

import com.application.employee.service.entities.Paystub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaystubRepository extends JpaRepository<Paystub, Long> {
    List<Paystub> findByEmployeeEmployeeID(String employeeId);
    List<Paystub> findByEmployeeEmployeeIDOrderByPayPeriodStartDesc(String employeeId);
    Optional<Paystub> findByIdAndEmployeeEmployeeID(Long id, String employeeId);
    void deleteByIdAndEmployeeEmployeeID(Long id, String employeeId);
    
    @Query("SELECT DISTINCT p FROM Paystub p LEFT JOIN FETCH p.employee")
    List<Paystub> findAllWithEmployee();
}

