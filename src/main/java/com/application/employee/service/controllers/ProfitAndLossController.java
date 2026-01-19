package com.application.employee.service.controllers;

import com.application.employee.service.entities.ProfitAndLoss;
import com.application.employee.service.services.ProfitAndLossService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profit-loss")
public class ProfitAndLossController {

    @Autowired
    private ProfitAndLossService profitAndLossService;

    @PostMapping("/{employeeId}")
    public ResponseEntity<ProfitAndLoss> createProfitAndLoss(
            @PathVariable String employeeId,
            @RequestBody ProfitAndLoss profitAndLoss) {
        ProfitAndLoss saved = profitAndLossService.saveProfitAndLoss(employeeId, profitAndLoss);
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<ProfitAndLoss>> getAllProfitAndLoss() {
        return ResponseEntity.ok(profitAndLossService.getAllProfitAndLoss());
    }

    @GetMapping("/employee/{employeeId}")
    public List<ProfitAndLoss> getProfitAndLossByEmployee(@PathVariable String employeeId) {
        return profitAndLossService.getByEmployeeId(employeeId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfitAndLoss> getProfitAndLossById(@PathVariable Long id) {
        return profitAndLossService.getProfitAndLossById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProfitAndLoss> updateProfitAndLoss(
            @PathVariable Long id,
            @RequestBody ProfitAndLoss profitAndLoss) {
        ProfitAndLoss updated = profitAndLossService.updateProfitAndLoss(id, profitAndLoss);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfitAndLoss(@PathVariable Long id) {
        profitAndLossService.deleteProfitAndLoss(id);
        return ResponseEntity.noContent().build();
    }
}

