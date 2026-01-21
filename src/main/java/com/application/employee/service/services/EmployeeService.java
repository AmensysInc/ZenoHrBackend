package com.application.employee.service.services;

import com.application.employee.service.dto.EmployeeDTO;
import com.application.employee.service.dto.ProspectFileDTO;
import com.application.employee.service.entities.Employee;
import com.application.employee.service.user.Role;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface EmployeeService {
    Employee saveEmployee(EmployeeDTO e);
    List<Employee> getAllEmployee();
    Employee getEmployee(String id);
    Employee updateEmployee(String id, EmployeeDTO e);
    void createProspectEmployee(Employee employee) throws IOException;
    void updateProspectEmployee(String id, Employee employee);
    void uploadProspectFiles(String employeeID, MultipartFile[] files) throws FileUploadException;
    void deleteEmployee(String id);
    Page<Employee> findEmployeeWithPagination(int page, int size, String field, String seacrhString);
    Page<Employee> findEmployeeWithPagination(int page, int size, String field, String seacrhString, Long companyId);
    List<Employee> getEmployeesBySecurityGroup(Role securityGroup);
    List<String> getProspectEmployeeFiles(String employeeID) throws IOException;
    byte[] downloadProspectEmployeeFile(String employeeID, String fileName) throws IOException;
    Map<String, List<String>> getAllEmployeesWithFiles() throws IOException;
    List<ProspectFileDTO> getAllProspectFiles();
    List<ProspectFileDTO> searchProspectFiles(String search);
    void deleteProspectFile(String employeeID, String fileName) throws IOException;
    List<Employee> getEmployeesByCompanyOrAll(Long company_id);
    
    // Weekly file upload methods
    void uploadWeeklyFiles(String employeeId, String week, MultipartFile file, String description) throws FileUploadException;
    List<String> getWeeklyFiles(String employeeId, String week) throws IOException;
    byte[] downloadWeeklyFile(String employeeId, String week, String fileName) throws IOException;
    void deleteWeeklyFile(String employeeId, String week, String fileName) throws IOException;
    List<Map<String, Object>> getAllWeeklyFiles() throws IOException;
}