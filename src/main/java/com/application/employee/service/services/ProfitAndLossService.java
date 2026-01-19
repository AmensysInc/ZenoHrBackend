package com.application.employee.service.services;

import com.application.employee.service.entities.ProfitAndLoss;

import java.util.List;
import java.util.Optional;

public interface ProfitAndLossService {
    ProfitAndLoss saveProfitAndLoss(String employeeId, ProfitAndLoss profitAndLoss);
    List<ProfitAndLoss> getByEmployeeId(String employeeID);
    List<ProfitAndLoss> getAllProfitAndLoss();
    Optional<ProfitAndLoss> getProfitAndLossById(Long id);
    ProfitAndLoss updateProfitAndLoss(Long id, ProfitAndLoss profitAndLoss);
    void deleteProfitAndLoss(Long id);
}
