package com.application.employee.service.controllers;

import com.application.employee.service.dto.EmployeeWithHoldDTO;
import com.application.employee.service.entities.CompanyBalanceProjection;
import com.application.employee.service.entities.WithHoldTracking;
import com.application.employee.service.services.WithHoldTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trackings")
@CrossOrigin(origins = "*")
public class WithHoldTrackingController {
    @Autowired
    private WithHoldTrackingService service;

    @GetMapping
    public ResponseEntity<Page<WithHoldTracking>> getAllWithHoldTracking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "searchField",defaultValue = "") String field,
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString
    ) {
        Page<WithHoldTracking> trackingList = service.findTrackingWithPagination(page, size, field, seacrhString);
        return ResponseEntity.ok(trackingList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WithHoldTracking> getWithHoldTrackingById(@PathVariable String id){
        WithHoldTracking tracking = service.getWithHoldTrackingById(id);
        return ResponseEntity.ok(tracking);
    }

    @PostMapping
    public ResponseEntity<WithHoldTracking> saveWithHoldTracking(@RequestBody WithHoldTracking withHoldTracking){
        WithHoldTracking tracking = service.saveWithHoldTracking(withHoldTracking);

        return ResponseEntity.status(HttpStatus.CREATED).body(tracking);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTracking(@PathVariable String id) {
        service.deleteTracking(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/company-balance")
    public List<CompanyBalanceProjection> getCompanyWiseBalance() {
        return service.getCompanyWiseBalance();
    }
    @GetMapping("/company/{companyId}")
    public List<EmployeeWithHoldDTO> getWithHoldByCompany(@PathVariable Integer companyId) {
        return service.getWithHoldByCompany(companyId);
    }
}
