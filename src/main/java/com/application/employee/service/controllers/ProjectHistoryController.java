package com.application.employee.service.controllers;

import com.application.employee.service.entities.ProjectHistory;
import com.application.employee.service.entities.PurchaseOrder;
import com.application.employee.service.services.ProjectHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@CrossOrigin(origins = "*")
public class ProjectHistoryController {
    @Autowired
    private ProjectHistoryService projectHistoryService;
    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectHistory> getProjectById(@PathVariable String projectId){
        ProjectHistory history = projectHistoryService.getProjectHistoryById(projectId);
        return ResponseEntity.ok(history);
    }
    @GetMapping
    public ResponseEntity<Page<ProjectHistory>> getAllProjectHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "searchField",defaultValue = "") String field,
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString
    ) {
        Page<ProjectHistory> historyList = projectHistoryService.findProjectWithPagination(page, size, field, seacrhString);
        return ResponseEntity.ok(historyList);
    }
    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProjectHistory(@PathVariable String projectId){
        projectHistoryService.deleteProjectHistory(projectId);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/bycompany")
    public ResponseEntity<Page<ProjectHistory>> getProjectsByCompany(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long companyId
    ) {
        Page<ProjectHistory> projects = projectHistoryService.findProjectsByCompanyId(page, size, companyId);
        return ResponseEntity.ok(projects);
    }

}
