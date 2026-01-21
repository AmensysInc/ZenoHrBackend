package com.application.employee.service.services;

import com.application.employee.service.dto.CompanyWithEmployeesDTO;
import com.application.employee.service.dto.TimeSheetDTO;
import com.application.employee.service.entities.Companies;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface CompaniesService {
    List<Companies> getAllCompanies();
    Companies getCompanyById(Integer companyId);
    Companies createCompany(Companies company);
    Companies updateCompany(Integer companyId, Companies company);
    void deleteCompany(Integer companyId);
    Page<Companies> findCompaniesWithPagination(int page, int size, String field, String seacrhString);
    Page<Companies> createPageFromList(List<Companies> content, int page, int size, long totalElements);
    List<CompanyWithEmployeesDTO> getCompaniesWithEmployees();
    void uploadCompanyDocument(Integer companyId, MultipartFile file) throws FileUploadException;
    byte[] downloadCompanyDocument(Integer companyId) throws IOException;
}
