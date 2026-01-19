package com.application.employee.service.repositories;

import com.application.employee.service.entities.ProfitAndLoss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProfitAndLossRepository extends JpaRepository<ProfitAndLoss, Long> {
    List<ProfitAndLoss> findByEmployee_EmployeeID(String employeeID);
}
