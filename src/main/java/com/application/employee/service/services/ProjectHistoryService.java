package com.application.employee.service.services;

import com.application.employee.service.entities.ProjectHistory;
import org.springframework.data.domain.Page;

import java.util.List;
public interface ProjectHistoryService {
    ProjectHistory saveProjectHistory(ProjectHistory projectHistory);
    ProjectHistory getProjectHistoryById(String id);
    List<ProjectHistory> getAllProjectHistory();
    ProjectHistory updateProjectHistory(String id, ProjectHistory updateProjectHistory);
    void deleteProjectHistory(String id);
    Page<ProjectHistory> findProjectWithPagination(int page, int size, String field, String seacrhString);
    Page<ProjectHistory> findProjectWithEmployeeID(int page, int size, String field, String seacrhString,String employeeID);
    Page<ProjectHistory> findProjectsByCompanyId(int page, int size, Long companyId);
}
