package com.application.employee.service.services.implementations;

import com.application.employee.service.dto.CompanyWithEmployeesDTO;
import com.application.employee.service.dto.EmployeeSummaryDTO;
import com.application.employee.service.dto.TimeSheetDTO;
import com.application.employee.service.dto.TimeSheetsDTO;
import com.application.employee.service.entities.Companies;
import com.application.employee.service.repositories.CompaniesRepository;
import com.application.employee.service.services.CompaniesService;
import com.application.employee.service.specifications.CompaniesSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CompanyServiceImpl implements CompaniesService {
    @Autowired
    private CompaniesRepository companiesRepository;

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
}
