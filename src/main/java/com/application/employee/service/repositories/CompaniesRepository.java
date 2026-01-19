package com.application.employee.service.repositories;

import com.application.employee.service.entities.Companies;
import com.application.employee.service.entities.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CompaniesRepository extends JpaRepository<Companies,Integer> {
    Page<Companies> findAll(Specification<Companies> spec, Pageable pageable);
    @Query("SELECT DISTINCT c FROM Companies c LEFT JOIN FETCH c.employees")
    List<Companies> findAllWithEmployees();

}
