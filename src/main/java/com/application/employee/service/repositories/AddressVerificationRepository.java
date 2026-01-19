package com.application.employee.service.repositories;

import com.application.employee.service.entities.AddressVerification;
import com.application.employee.service.entities.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AddressVerificationRepository extends JpaRepository<AddressVerification, Long> {
    Optional<AddressVerification> findByEmployee(Employee employee);
    Optional<AddressVerification> findByEmployeeEmployeeID(String employeeId);
}

