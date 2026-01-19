package com.application.employee.service.controllers;

import com.application.employee.service.dto.CompanyWithEmployeesDTO;
import com.application.employee.service.dto.TimeSheetDTO;
import com.application.employee.service.entities.Candidate;
import com.application.employee.service.entities.Companies;
import com.application.employee.service.services.CompaniesService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/companies")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class CompaniesController {
    @Autowired
    private CompaniesService companiesService;
    @PostMapping
    public Companies createCompany(@RequestBody Companies company) {
        return
                companiesService.createCompany(company);
    }
    @GetMapping
    public ResponseEntity<Page<Companies>> getAllCompanies(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "searchField",defaultValue = "") String field,
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString
    ) {
        Page<Companies> companies = companiesService.findCompaniesWithPagination(page, size, field, seacrhString);
        return ResponseEntity.ok(companies);
    }
    @GetMapping("/{companyId}")
    public Companies getCompanyById(@PathVariable Integer companyId) {
        return companiesService.getCompanyById(companyId);
    }
    @PutMapping("/{companyId}")
    public Companies updateCompany(@PathVariable Integer companyId, @RequestBody Companies company) {
        return companiesService.updateCompany(companyId, company);
    }

    @DeleteMapping("/{companyId}")
    public void deleteCompany(@PathVariable Integer companyId) {
        companiesService.deleteCompany(companyId);
    }

    @GetMapping("/companies-with-employees")
    public List<CompanyWithEmployeesDTO> getCompaniesWithEmployees() {
        return companiesService.getCompaniesWithEmployees();
    }

}
