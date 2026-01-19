package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.ProjectHistory;
import com.application.employee.service.entities.PurchaseOrder;
import com.application.employee.service.exceptions.ResourceNotFoundException;
import com.application.employee.service.repositories.ProjectHistoryRepository;
import com.application.employee.service.services.ProjectHistoryService;
import com.application.employee.service.specifications.ProjectHistorySpecifications;
import com.application.employee.service.specifications.PurchaseOrderSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;
@Service
public class ProjectHistoryServiceImpl implements ProjectHistoryService {
    @Autowired
    private ProjectHistoryRepository projectHistoryRepository;
    @Override
    public ProjectHistory saveProjectHistory(ProjectHistory projectHistory) {
        String randomProjectID = UUID.randomUUID().toString();
        projectHistory.setProjectId(randomProjectID);
        return projectHistoryRepository.save(projectHistory);
    }

    @Override
    public ProjectHistory getProjectHistoryById(String id) {
        return projectHistoryRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Project History not found with given ID: " + id)
        );
    }

    @Override
    public List<ProjectHistory> getAllProjectHistory() {
        return projectHistoryRepository.findAll();
    }

    @Override
    public ProjectHistory updateProjectHistory(String id, ProjectHistory updateProjectHistory) {
        ProjectHistory existingProject = getProjectHistoryById(id);
        existingProject.setSubVendorOne(updateProjectHistory.getSubVendorOne());
        existingProject.setSubVendorTwo(updateProjectHistory.getSubVendorTwo());
        existingProject.setProjectAddress(updateProjectHistory.getProjectAddress());
        existingProject.setProjectStartDate(updateProjectHistory.getProjectStartDate());
        existingProject.setProjectEndDate(updateProjectHistory.getProjectEndDate());
        existingProject.setProjectStatus(updateProjectHistory.getProjectStatus());
        existingProject.setNetTerms(updateProjectHistory.getNetTerms());
        existingProject.setRecruiterName(updateProjectHistory.getRecruiterName());
        return projectHistoryRepository.save(existingProject);
    }

    @Override
    public void deleteProjectHistory(String id) {
        ProjectHistory history = getProjectHistoryById(id);
        projectHistoryRepository.delete(history);
    }

    @Override
    public Page<ProjectHistory> findProjectWithPagination(int page, int size, String searchField, String searchString) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "projectStartDate"));
        Specification<ProjectHistory> spec = Specification.where(null);

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "subVendorOne": spec = spec.and(ProjectHistorySpecifications.subVendorOneEquals(searchString)); break;
                case "subVendorTwo": spec = spec.and(ProjectHistorySpecifications.subVendorTwoEquals(searchString)); break;
                case "projectAddress": spec = spec.and(ProjectHistorySpecifications.projectAddressEquals(searchString)); break;
                case "projectStartDate": spec = spec.and(ProjectHistorySpecifications.projectStartDateEquals(searchString)); break;
                case "projectEndDate": spec = spec.and(ProjectHistorySpecifications.projectEndDateEquals(searchString)); break;
                case "projectStatus": spec = spec.and(ProjectHistorySpecifications.projectStatusEquals(searchString)); break;
            }
        }

        return projectHistoryRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ProjectHistory> findProjectWithEmployeeID(int page, int size, String searchField, String searchString, String employeeID) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<ProjectHistory> spec = Specification.where(null);

        if (!employeeID.isEmpty()){
            spec = spec.and(ProjectHistorySpecifications.employeeIDEquals(employeeID));
        }

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "subVendorOne":
                    spec = spec.and(ProjectHistorySpecifications.subVendorOneEquals(searchString));
                    break;
                case "subVendorTwo":
                    spec = spec.and(ProjectHistorySpecifications.subVendorTwoEquals(searchString));
                    break;
                case "projectAddress":
                    spec = spec.and(ProjectHistorySpecifications.projectAddressEquals(searchString));
                    break;
                case "projectStartDate":
                    spec = spec.and(ProjectHistorySpecifications.projectStartDateEquals(searchString));
                    break;
                case "projectEndDate":
                    spec = spec.and(ProjectHistorySpecifications.projectEndDateEquals(searchString));
                    break;
                case "projectStatus":
                    spec = spec.and(ProjectHistorySpecifications.projectStatusEquals(searchString));
                    break;
            }
        }

        return projectHistoryRepository.findAll(spec, pageable);
    }

    @Override
    public Page<ProjectHistory> findProjectsByCompanyId(int page, int size, Long companyId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "projectStartDate"));
        return projectHistoryRepository.findByCompanyIdOrAll(companyId, pageable);
    }

}
