package com.application.employee.service.repositories;

import com.application.employee.service.entities.YTDData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface YTDDataRepository extends JpaRepository<YTDData, Long> {
    
    @Query("SELECT y FROM YTDData y LEFT JOIN FETCH y.employee WHERE y.employee.employeeID = :employeeId AND y.currentYear = :year")
    Optional<YTDData> findByEmployeeEmployeeIDAndCurrentYear(@Param("employeeId") String employeeId, @Param("year") Integer year);
    
    @Query("SELECT y FROM YTDData y LEFT JOIN FETCH y.employee WHERE y.employee.employeeID = :employeeId")
    Optional<YTDData> findByEmployeeEmployeeID(@Param("employeeId") String employeeId);
}

