package com.application.employee.service.repositories;

import com.application.employee.service.dto.EmployeeDTO;
import com.application.employee.service.entities.Employee;
import com.application.employee.service.user.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRespository extends JpaRepository<Employee, String> {
    Optional<Employee> findByEmailID(String emailId);
    Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);
    List<Employee> findBySecurityGroup(Role securityGroup);

    @Query("SELECT e FROM Employee e WHERE (:CompanyId IS NULL OR e.CompanyId = :CompanyId)")
    List<Employee> findByCompanyOrAll(@Param("CompanyId") Long company_id);

    @Query("SELECT e.company.email FROM Employee e WHERE e.emailID = :email")
    String findCompanyEmailByEmployeeEmail(String email);
}