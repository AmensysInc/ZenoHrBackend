package com.application.employee.service.controllers;

import com.application.employee.service.dto.CompanyWithEmployeesDTO;
import com.application.employee.service.dto.TimeSheetDTO;
import com.application.employee.service.entities.Candidate;
import com.application.employee.service.entities.Companies;
import com.application.employee.service.entities.UserCompanyRole;
import com.application.employee.service.repositories.UserCompanyRoleRepository;
import com.application.employee.service.repositories.UserRepository;
import com.application.employee.service.services.CompaniesService;
import com.application.employee.service.user.Role;
import com.application.employee.service.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/companies")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class CompaniesController {
    @Autowired
    private CompaniesService companiesService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;
    
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
        // Get current user
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        
        Page<Companies> companies = companiesService.findCompaniesWithPagination(page, size, field, seacrhString);
        
        // Filter companies based on role
        if (currentUser != null && currentUser.getRole() == Role.ADMIN) {
            // ADMIN: Only show companies assigned to them
            List<UserCompanyRole> adminRoles = userCompanyRoleRepository.findByUserId(currentUser.getId());
            Set<Integer> adminCompanyIds = adminRoles.stream()
                    .map(UserCompanyRole::getCompanyId)
                    .collect(Collectors.toSet());
            
            // Filter the page content
            List<Companies> filteredContent = companies.getContent().stream()
                    .filter(company -> adminCompanyIds.contains(company.getCompanyId()))
                    .collect(Collectors.toList());
            
            // Get total count of filtered companies (need to fetch all and count)
            long totalFiltered = companiesService.findCompaniesWithPagination(0, Integer.MAX_VALUE, field, seacrhString)
                    .getContent().stream()
                    .filter(company -> adminCompanyIds.contains(company.getCompanyId()))
                    .count();
            
            // Create a new page with filtered content
            return ResponseEntity.ok(companiesService.createPageFromList(filteredContent, page, size, totalFiltered));
        }
        // SADMIN: Return all companies (no filtering)
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
