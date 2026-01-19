package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.ProfitAndLoss;
import com.application.employee.service.repositories.EmployeeRespository;
import com.application.employee.service.repositories.ProfitAndLossRepository;
import com.application.employee.service.services.ProfitAndLossService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProfitAndLossServiceImpl implements ProfitAndLossService {

    @Autowired
    private ProfitAndLossRepository profitAndLossRepository;

    @Autowired
    private EmployeeRespository employeeRepository;

    @Override
    public ProfitAndLoss saveProfitAndLoss(String employeeId, ProfitAndLoss profitAndLoss) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found with ID: " + employeeId));

        profitAndLoss.setEmployee(employee);
        return profitAndLossRepository.save(profitAndLoss);
    }

    @Override
    public List<ProfitAndLoss> getByEmployeeId(String employeeID) {
        return profitAndLossRepository.findByEmployee_EmployeeID(employeeID);
    }

    @Override
    public List<ProfitAndLoss> getAllProfitAndLoss() {
        return profitAndLossRepository.findAll();
    }

    @Override
    public Optional<ProfitAndLoss> getProfitAndLossById(Long id) {
        return profitAndLossRepository.findById(id);
    }

    @Override
    public ProfitAndLoss updateProfitAndLoss(Long id, ProfitAndLoss profitAndLoss) {
        return profitAndLossRepository.findById(id)
                .map(existing -> {
                    existing.setDate(profitAndLoss.getDate());
                    existing.setType(profitAndLoss.getType());
                    existing.setHours(profitAndLoss.getHours());
                    existing.setRate(profitAndLoss.getRate());
                    existing.setAmount(profitAndLoss.getAmount());
                    existing.setOtherAmount(profitAndLoss.getOtherAmount());
                    existing.setTotalAmount(profitAndLoss.getTotalAmount());
                    existing.setStatus(profitAndLoss.getStatus());
                    existing.setNotes(profitAndLoss.getNotes());
                    // Optionally update employee if required
                    return profitAndLossRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("ProfitAndLoss not found"));
    }

    @Override
    public void deleteProfitAndLoss(Long id) {
        profitAndLossRepository.deleteById(id);
    }
}

