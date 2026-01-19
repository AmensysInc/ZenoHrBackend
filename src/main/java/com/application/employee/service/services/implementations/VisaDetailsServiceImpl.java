package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.ProjectHistory;
import com.application.employee.service.entities.VisaDetails;
import com.application.employee.service.exceptions.ResourceNotFoundException;
import com.application.employee.service.repositories.VisaDetailsRepository;
import com.application.employee.service.services.VisaDetailsService;
import com.application.employee.service.specifications.ProjectHistorySpecifications;
import com.application.employee.service.specifications.PurchaseOrderSpecifications;
import com.application.employee.service.specifications.VisaDetailsSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Service
public class VisaDetailsServiceImpl implements VisaDetailsService {
    @Autowired
    private VisaDetailsRepository visaDetailsRepository;

    @Override
    public VisaDetails saveDetails(VisaDetails details) {
        String randomVisaID = UUID.randomUUID().toString();
        details.setVisaId(randomVisaID);
        return visaDetailsRepository.save(details);
    }

    @Override
    public List<VisaDetails> getAllDetails() {
        return visaDetailsRepository.findAll();
    }

    @Override
    public VisaDetails getVisaDetailsById(String id) {
        return visaDetailsRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Visa Details not found with given id" + id)
        );
    }

    @Override
    public VisaDetails updateVisaDetails(String id, VisaDetails updateVisaDetails) {
        VisaDetails existingDetails = getVisaDetailsById(id);

        existingDetails.setVisaType(updateVisaDetails.getVisaType());
        existingDetails.setVisaStartDate(updateVisaDetails.getVisaStartDate());
        existingDetails.setVisaExpiryDate(updateVisaDetails.getVisaExpiryDate());
        existingDetails.setI94Date(updateVisaDetails.getI94Date());
        existingDetails.setLcaAddress(updateVisaDetails.getLcaAddress());
        existingDetails.setLcaNumber(updateVisaDetails.getLcaNumber());
        existingDetails.setLcaWage(updateVisaDetails.getLcaWage());
        existingDetails.setJobTitle(updateVisaDetails.getJobTitle());
        existingDetails.setI140Status(updateVisaDetails.getI140Status());
        existingDetails.setGcStatus(updateVisaDetails.getGcStatus());
        existingDetails.setAttorney(updateVisaDetails.getAttorney());
        existingDetails.setReceipt(updateVisaDetails.getReceipt());
        existingDetails.setResidentialAddress(updateVisaDetails.getResidentialAddress());
        existingDetails.setComments(updateVisaDetails.getComments());

        return visaDetailsRepository.save(existingDetails);
    }

    @Override
    public void deleteVisaDetails(String id) {
        VisaDetails details = getVisaDetailsById(id);
        visaDetailsRepository.delete(details);
    }

    @Override
    public Page<VisaDetails> findVisaDetailsWithPagination(int page, int size, String searchField, String searchString) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<VisaDetails> spec = Specification.where(null);

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "visaType":
                    spec = spec.and(VisaDetailsSpecifications.visaTypeEquals(searchString));
                    break;
                case "visaStartDate":
                    spec = spec.and(VisaDetailsSpecifications.visaStartDateEquals(searchString));
                    break;
                case "visaExpiryDate":
                    spec = spec.and(VisaDetailsSpecifications.visaExpiryDateEquals(searchString));
                    break;
            }
        }
        return visaDetailsRepository.findAll(spec, pageable);
    }

    @Override
    public Page<VisaDetails> findVisaDetailsWithEmployeeID(int page, int size, String searchField, String searchString, String employeeID) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<VisaDetails> spec = Specification.where(null);

        if (!employeeID.isEmpty()){
            spec = spec.and(VisaDetailsSpecifications.employeeIDEquals(employeeID));
        }

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "visaType":
                    spec = spec.and(VisaDetailsSpecifications.visaTypeEquals(searchString));
                    break;
                case "visaStartDate":
                    spec = spec.and(VisaDetailsSpecifications.visaStartDateEquals(searchString));
                    break;
                case "visaExpiryDate":
                    spec = spec.and(VisaDetailsSpecifications.visaExpiryDateEquals(searchString));
                    break;
            }
        }
        return visaDetailsRepository.findAll(spec, pageable);
    }

    @Override
    public Page<VisaDetails> findVisaDetailsByCompanyId(int page, int size, Long companyId) {
        Pageable pageable = PageRequest.of(page, size);
        return visaDetailsRepository.findByCompanyIdOrAll(companyId, pageable);
    }

}
