package com.application.employee.service.repositories;


import com.application.employee.service.entities.ProspectFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProspectFileRepository extends JpaRepository<ProspectFile, String> {
    List<ProspectFile> findByEmployee_EmployeeID(String employeeID);
}
