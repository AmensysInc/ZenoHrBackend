package com.application.employee.service.repositories;

import com.application.employee.service.entities.BulkMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BulkMailRepository extends JpaRepository<BulkMail, Long> {
    @Query("SELECT e.email FROM BulkMail e")
    List<String> findEmails();
    @Query("SELECT e FROM BulkMail e WHERE e.recruiterId = :recruiterId")
    List<BulkMail> findByRecruiterId(@Param("recruiterId") String recruiterId);
}