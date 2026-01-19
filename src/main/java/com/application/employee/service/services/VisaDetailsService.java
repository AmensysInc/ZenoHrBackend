package com.application.employee.service.services;

import com.application.employee.service.entities.VisaDetails;
import org.springframework.data.domain.Page;

import java.util.List;

public interface VisaDetailsService {
    VisaDetails saveDetails(VisaDetails details);
    List<VisaDetails> getAllDetails();
    VisaDetails getVisaDetailsById(String id);
    VisaDetails updateVisaDetails(String id, VisaDetails updateVisaDetails);
    void deleteVisaDetails(String id);
    Page<VisaDetails> findVisaDetailsWithPagination(int page, int size, String field, String seacrhString);
    Page<VisaDetails> findVisaDetailsWithEmployeeID(int page, int size, String field, String seacrhString, String employeeID);
    Page<VisaDetails> findVisaDetailsByCompanyId(int page, int size, Long companyId);

}
