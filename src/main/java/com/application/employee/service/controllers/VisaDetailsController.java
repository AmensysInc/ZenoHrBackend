package com.application.employee.service.controllers;

import com.application.employee.service.entities.VisaDetails;
import com.application.employee.service.services.VisaDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/visa-details")
@CrossOrigin(origins = "*")
public class VisaDetailsController {
    @Autowired
    private VisaDetailsService visaDetailsService;

    @GetMapping("/{visaDetailsId}")
    public ResponseEntity<VisaDetails> getDetailsById(@PathVariable String visaDetailsId){
        VisaDetails details = visaDetailsService.getVisaDetailsById(visaDetailsId);
        return ResponseEntity.ok(details);
    }
    @GetMapping
    public ResponseEntity<Page<VisaDetails>> getAllVisaDetails(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "searchField",defaultValue = "") String field,
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString
    ) {
        Page<VisaDetails> detailsList = visaDetailsService.findVisaDetailsWithPagination(page, size, field, seacrhString);
        return ResponseEntity.ok(detailsList);
    }
    @DeleteMapping("/{visaDetailsId}")
    public ResponseEntity<Void> deleteVisaDetails(@PathVariable String visaDetailsId){
        visaDetailsService.deleteVisaDetails(visaDetailsId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bycompany")
    public ResponseEntity<Page<VisaDetails>> getVisaDetailsByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long companyId
    ) {
        Page<VisaDetails> details = visaDetailsService.findVisaDetailsByCompanyId(page, size, companyId);
        return ResponseEntity.ok(details);
    }

}
