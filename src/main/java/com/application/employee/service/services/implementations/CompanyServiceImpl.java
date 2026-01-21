package com.application.employee.service.services.implementations;

import com.application.employee.service.dto.CompanyWithEmployeesDTO;
import com.application.employee.service.dto.EmployeeSummaryDTO;
import com.application.employee.service.dto.TimeSheetDTO;
import com.application.employee.service.dto.TimeSheetsDTO;
import com.application.employee.service.entities.Companies;
import com.application.employee.service.repositories.CompaniesRepository;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import com.application.employee.service.services.CompaniesService;
import com.application.employee.service.specifications.CompaniesSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CompanyServiceImpl implements CompaniesService {
    @Autowired
    private CompaniesRepository companiesRepository;
    
    @Value("${file.storage-location}")
    private String uploadPath;

    @Override
    public List<Companies> getAllCompanies() {
        return companiesRepository.findAll();
    }

    @Override
    public Companies getCompanyById(Integer companyId) {
        Optional<Companies> optionalCompany = companiesRepository.findById(companyId);
        return optionalCompany.orElse(null);
    }

    @Override
    public Companies createCompany(Companies company) {
        return companiesRepository.save(company);
    }

    @Override
    public Companies updateCompany(Integer companyId, Companies company) {
        if (companiesRepository.existsById(companyId)) {
            company.setCompanyId(companyId);
            return companiesRepository.save(company);
        }
        return null;
    }

    @Override
    public void deleteCompany(Integer companyId) {
        companiesRepository.deleteById(companyId);
    }

    @Override
    public Page<Companies> findCompaniesWithPagination(int page, int size, String searchField, String searchString) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<Companies> spec = Specification.where(null);

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "companyName":
                    spec = spec.and(CompaniesSpecifications.companyNameEquals(searchString));
                    break;
                case "email":
                    spec = spec.and(CompaniesSpecifications.emailEquals(searchString));
                    break;

            }
        }

        return companiesRepository.findAll(spec, pageable);
    }

    @Override
    public Page<Companies> createPageFromList(List<Companies> content, int page, int size, long totalElements) {
        Pageable pageable = PageRequest.of(page, size);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), content.size());
        List<Companies> pageContent = content.subList(start, end);
        return new PageImpl<>(pageContent, pageable, totalElements);
    }

    @Override
    public List<CompanyWithEmployeesDTO> getCompaniesWithEmployees() {
        List<Companies> companies = companiesRepository.findAllWithEmployees();

        return companies.stream()
                .map(c -> new CompanyWithEmployeesDTO(
                        c.getCompanyId(),
                        c.getCompanyName(),
                        c.getEmployees().stream()
                                .map(e -> new EmployeeSummaryDTO(
                                        e.getEmployeeID(),
                                        e.getFirstName(),
                                        e.getLastName()
                                ))
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void uploadCompanyDocument(Integer companyId, MultipartFile file) throws FileUploadException {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("No file provided for upload.");
        }

        try {
            // Get company
            Companies company = companiesRepository.findById(companyId)
                    .orElseThrow(() -> new FileUploadException("Company not found with ID: " + companyId));

            // Create directory for company documents
            Path companyUploadPath = Paths.get(uploadPath, "companies", String.valueOf(companyId));
            if (!Files.exists(companyUploadPath)) {
                Files.createDirectories(companyUploadPath);
            }

            // Save file
            String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), "File name is null");
            String safeFileName = Paths.get(originalFilename).getFileName().toString(); // Remove path info
            Path filePath = companyUploadPath.resolve(safeFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Update company with document info
            company.setDocumentName(safeFileName);
            company.setDocumentPath(filePath.toString());
            companiesRepository.save(company);

        } catch (IOException e) {
            throw new FileUploadException("Failed to upload document: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] downloadCompanyDocument(Integer companyId) throws IOException {
        Companies company = companiesRepository.findById(companyId)
                .orElseThrow(() -> new IOException("Company not found with ID: " + companyId));

        if (company.getDocumentPath() == null || company.getDocumentPath().isEmpty()) {
            throw new IOException("No document found for company: " + companyId);
        }

        Path filePath = Paths.get(company.getDocumentPath());
        if (!Files.exists(filePath)) {
            throw new IOException("Document file not found at path: " + company.getDocumentPath());
        }

        return Files.readAllBytes(filePath);
    }
}
