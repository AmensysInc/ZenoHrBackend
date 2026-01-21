package com.application.employee.service.controllers;

import com.application.employee.service.dto.CompanyWithEmployeesDTO;
import com.application.employee.service.dto.TimeSheetDTO;
import com.application.employee.service.entities.Candidate;
import com.application.employee.service.entities.Companies;
import com.application.employee.service.entities.UserCompanyRole;
import com.application.employee.service.repositories.UserCompanyRoleRepository;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import com.application.employee.service.repositories.UserRepository;
import com.application.employee.service.services.CompaniesService;
import com.application.employee.service.user.Role;
import com.application.employee.service.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString,
            @RequestParam(name = "companyId", required = false) Integer companyId
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
        } else if (currentUser != null && currentUser.getRole() == Role.GROUP_ADMIN && companyId != null) {
            // GROUP_ADMIN: Only show the selected company
            List<Companies> filteredContent = companies.getContent().stream()
                    .filter(company -> company.getCompanyId().equals(companyId))
                    .collect(Collectors.toList());
            
            long totalFiltered = companiesService.findCompaniesWithPagination(0, Integer.MAX_VALUE, field, seacrhString)
                    .getContent().stream()
                    .filter(company -> company.getCompanyId().equals(companyId))
                    .count();
            
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

    @PostMapping("/{companyId}/upload-document")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN') or hasRole('GROUP_ADMIN')")
    public ResponseEntity<String> uploadCompanyDocument(
            @PathVariable Integer companyId,
            @RequestParam("file") MultipartFile file) {
        try {
            companiesService.uploadCompanyDocument(companyId, file);
            return ResponseEntity.ok("Document uploaded successfully");
        } catch (FileUploadException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload document: " + e.getMessage());
        }
    }

    @GetMapping("/{companyId}/download-document")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN') or hasRole('GROUP_ADMIN')")
    public ResponseEntity<byte[]> downloadCompanyDocument(@PathVariable Integer companyId) {
        try {
            byte[] fileData = companiesService.downloadCompanyDocument(companyId);
            Companies company = companiesService.getCompanyById(companyId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", company.getDocumentName());
            headers.setContentLength(fileData.length);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

}
