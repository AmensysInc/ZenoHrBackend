package com.application.employee.service.repositories;

import com.application.employee.service.entities.Contacts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ContactsRepository extends JpaRepository<Contacts, Long> {
    @Query("SELECT e.email FROM Contacts e")
    List<String> findEmails();
    @Query("SELECT e FROM Contacts e WHERE e.recruiterId = :recruiterId")
    List<Contacts> findByRecruiterId(@Param("recruiterId") String recruiterId);
}
