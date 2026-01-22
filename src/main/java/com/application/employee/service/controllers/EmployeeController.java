package com.application.employee.service.controllers;

import com.application.employee.service.dto.EmployeeDTO;
import com.application.employee.service.dto.ProspectFileDTO;
import com.application.employee.service.entities.*;
import com.application.employee.service.repositories.UserCompanyRoleRepository;
import com.application.employee.service.repositories.UserRepository;
import com.application.employee.service.services.*;
import com.application.employee.service.user.Role;
import com.application.employee.service.user.User;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/employees")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class EmployeeController {
    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private PurchaseOrderService purchaseOrderService;
    @Autowired
    private WithHoldTrackingService withHoldTrackingService;
    @Autowired
    private ProjectHistoryService projectHistoryService;
    @Autowired
    private VisaDetailsService visaDetailsService;
    @Value("${file.storage-location}")
    private String UploadPath;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN') or hasRole('GROUP_ADMIN')")
    public ResponseEntity<String> createEmployee(@RequestBody EmployeeDTO employeeDTO) {
        // ✅ If Admin creates employee without company, auto-assign to Admin's company
        // Get current user from SecurityContext
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        
        if (currentUser != null) {
            if (currentUser.getRole() == Role.ADMIN) {
                // Admin user - get their default company
                List<UserCompanyRole> adminRoles = userCompanyRoleRepository.findByUserId(currentUser.getId());
                UserCompanyRole defaultRole = adminRoles.stream()
                        .filter(role -> "true".equalsIgnoreCase(role.getDefaultCompany()))
                        .findFirst()
                        .orElse(adminRoles.isEmpty() ? null : adminRoles.get(0));
                
                if (defaultRole != null && employeeDTO.getCompanyId() == null) {
                    // Auto-assign employee to Admin's company
                    employeeDTO.setCompanyId(defaultRole.getCompanyId());
                }
            } else if (currentUser.getRole() == Role.GROUP_ADMIN) {
                // GROUP_ADMIN - validate that companyId is one of their assigned companies
                List<UserCompanyRole> groupAdminRoles = userCompanyRoleRepository.findByUserId(currentUser.getId());
                List<Integer> assignedCompanyIds = groupAdminRoles.stream()
                        .map(UserCompanyRole::getCompanyId)
                        .toList();
                
                // If no companyId provided, use selected company from session
                if (employeeDTO.getCompanyId() == null) {
                    // Try to get selected company (frontend should send it, but if not, use first assigned)
                    UserCompanyRole defaultRole = groupAdminRoles.stream()
                            .filter(role -> "true".equalsIgnoreCase(role.getDefaultCompany()))
                            .findFirst()
                            .orElse(groupAdminRoles.isEmpty() ? null : groupAdminRoles.get(0));
                    if (defaultRole != null) {
                        employeeDTO.setCompanyId(defaultRole.getCompanyId());
                    }
                } else {
                    // Validate that the provided companyId is one of their assigned companies
                    if (!assignedCompanyIds.contains(employeeDTO.getCompanyId())) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body("You can only assign employees to companies you manage");
                    }
                }
            }
        }
        // SADMIN can assign to any company or leave null (will be handled in service)
        
        Employee employee = employeeService.saveEmployee(employeeDTO);
        if (employee == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Employee already exists for given EmailID");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("Employee created successfully");
    }
    @GetMapping("/{employeeID}")
    public ResponseEntity<Employee> getEmployeeByID(@PathVariable String employeeID) {
        Employee employee = employeeService.getEmployee(employeeID);
        return ResponseEntity.ok(employee);
    }
    @GetMapping("/role")
    public ResponseEntity<List<Employee>> getEmployeesBySecurityGroup(@RequestParam("securityGroup") String securityGroup) {
        try {
            Role role = Role.valueOf(securityGroup);
            List<Employee> employees = employeeService.getEmployeesBySecurityGroup(role);
            return ResponseEntity.ok(employees);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Collections.emptyList());
        }
    }

    @GetMapping("/bycompany")
    public List<Employee> getEmployeesByCompanyOrAll(
            @RequestParam(required = false) Long company_id) {
        return employeeService.getEmployeesByCompanyOrAll(company_id);
    }

    @GetMapping
    public ResponseEntity<Page<Employee>> getAllEmployee(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "searchField",defaultValue = "") String field,
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString,
            @RequestParam(name = "company_id", required = false) Long companyId
    ) {
        // Get current user
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        
        // For GROUP_ADMIN, filter by selected company
        if (currentUser != null && currentUser.getRole() == Role.GROUP_ADMIN) {
            // Get GROUP_ADMIN's assigned companies
            List<UserCompanyRole> userRoles = userCompanyRoleRepository.findByUserId(currentUser.getId());
            List<Integer> assignedCompanyIds = userRoles.stream()
                    .map(UserCompanyRole::getCompanyId)
                    .toList();
            
            // If company_id is provided, validate it's one of their assigned companies
            if (companyId != null && assignedCompanyIds.contains(companyId.intValue())) {
                // Valid company - use it for filtering
            } else if (companyId != null) {
                // Invalid company - return error or use first assigned
                // For now, use first assigned company as fallback
                if (!assignedCompanyIds.isEmpty()) {
                    companyId = assignedCompanyIds.get(0).longValue();
                } else {
                    // No companies assigned - return empty result
                    companyId = -1L; // Will result in no matches
                }
            } else if (!assignedCompanyIds.isEmpty()) {
                // No company_id provided - use selected/default company
                UserCompanyRole defaultRole = userRoles.stream()
                        .filter(role -> "true".equalsIgnoreCase(role.getDefaultCompany()))
                        .findFirst()
                        .orElse(userRoles.get(0));
                companyId = defaultRole.getCompanyId().longValue();
            } else {
                // No companies assigned - return empty result
                companyId = -1L;
            }
        }
        
        // For SADMIN, ignore company_id filter (show all)
        if (currentUser != null && currentUser.getRole() == Role.SADMIN) {
            companyId = null;
        }
        
        // For REPORTING_MANAGER, filter by reportingManagerId
        String reportingManagerId = null;
        if (currentUser != null && currentUser.getRole() == Role.REPORTING_MANAGER) {
            reportingManagerId = currentUser.getId();
        }
        
        Page<Employee> employees = employeeService.findEmployeeWithPagination(page, size, field, seacrhString, companyId, reportingManagerId);
        return ResponseEntity.ok(employees);
    }

    @PutMapping("/{employeeID}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN') or hasRole('GROUP_ADMIN') or hasRole('REPORTING_MANAGER')")
    public ResponseEntity<String> updateEmployee(@PathVariable String employeeID, @RequestBody EmployeeDTO employeeDTO) {
        employeeService.updateEmployee(employeeID, employeeDTO);
        return ResponseEntity.ok("Employee updated successfully");
    }

    @PostMapping("/prospect")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN') or hasRole('PROSPECT')")
    public ResponseEntity<String> createProspectEmployee(@RequestBody Employee employee) throws IOException {
        employeeService.createProspectEmployee(employee);
        return ResponseEntity.ok("Prospect added successfully");
    }

    @PutMapping("/prospect/{employeeID}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN') or hasRole('PROSPECT')")
    public ResponseEntity<String> updateProspectEmployee(@PathVariable String employeeID, @RequestBody EmployeeDTO employeeDTO) {

        Employee employee = new Employee(employeeDTO);
        employee.setEmployeeID(employeeID);
        EmployeeDetails employeeDetails = new EmployeeDetails(employeeDTO);
        employeeDetails.setEmployeeDetailsID(employeeID);
        employee.setEmployeeDetails(employeeDetails);
        employeeService.updateProspectEmployee(employeeID, employee);

        return ResponseEntity.ok("Prospect updated successfully");
    }

    @PostMapping("/prospectFiles/{employeeID}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN') or hasRole('PROSPECT') or hasRole('EMPLOYEE')")
    public ResponseEntity<String> updateProspectEmployee(
            @PathVariable String employeeID,
            @RequestParam("documents") MultipartFile[] files) {
        try {
            employeeService.uploadProspectFiles(employeeID, files);
            return ResponseEntity.ok("Prospect Employee files uploaded successfully");
        } catch (FileUploadException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload files.");
        }
    }

    @GetMapping("/prospectFiles/{employeeID}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN') or hasRole('PROSPECT') or hasRole('EMPLOYEE')")
    public ResponseEntity<List<String>> listProspectFiles(@PathVariable String employeeID) {
        try {
            List<String> files = employeeService.getProspectEmployeeFiles(employeeID);
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(List.of());
        }
    }

    @GetMapping("/prospectFiles/{employeeID}/{fileName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN') or hasRole('PROSPECT') or hasRole('EMPLOYEE')")
    public ResponseEntity<byte[]> downloadProspectFile(
            @PathVariable String employeeID,
            @PathVariable String fileName) {
        try {
            byte[] fileData = employeeService.downloadProspectEmployeeFile(employeeID, fileName);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .body(fileData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/prospectFiles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllEmployeesWithFiles() {
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            List<Employee> employees = employeeService.getAllEmployee();

            for (Employee emp : employees) {
                Map<String, Object> empData = new HashMap<>();
                empData.put("employeeID", emp.getEmployeeID());
                empData.put("firstName", emp.getFirstName());
                empData.put("lastName", emp.getLastName());
                empData.put("files", employeeService.getProspectEmployeeFiles(emp.getEmployeeID()));
                result.add(empData);
            }

            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }

    @GetMapping("/prospectFiles/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllProspectFiles(
            @RequestParam(name = "search", required = false) String search) {

        List<ProspectFileDTO> files;

        if (search != null && !search.trim().isEmpty()) {
            files = employeeService.searchProspectFiles(search);
        } else {
            files = employeeService.getAllProspectFiles();
        }

        List<Map<String, Object>> response = files.stream().map(file -> {
            Map<String, Object> fileData = new HashMap<>();
            fileData.put("fileName", file.getFileName());
            fileData.put("uploadedBy", file.getFirstName() + " " + file.getLastName());
            fileData.put("employeeID", file.getEmployeeId());
            fileData.put("uploadTime", file.getUploadTime());
            return fileData;
        }).toList();

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/prospectFiles/{employeeID}/{fileName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN') or hasRole('PROSPECT')")
    public ResponseEntity<String> deleteProspectFile(
            @PathVariable String employeeID,
            @PathVariable String fileName) {
        try {
            employeeService.deleteProspectFile(employeeID, fileName);
            return ResponseEntity.ok("File deleted successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found or could not be deleted.");
        }
    }

    @DeleteMapping("/{employeeID}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ResponseEntity<String> deleteEmployee(@PathVariable String employeeID) {
        employeeService.deleteEmployee(employeeID);
        return ResponseEntity.ok("Employee deleted successfully");
    }

    @PostMapping("/{employeeId}/orders")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public PurchaseOrder createOrder(@PathVariable(value = "employeeId") String employeeId, @RequestBody PurchaseOrder order) {
        Employee employee = employeeService.getEmployee(employeeId);
        order.setEmployee(employee);
        return purchaseOrderService.saveOrder(order);
    }
    @PutMapping("/orders/{orderID}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ResponseEntity<PurchaseOrder> updateOrder(
            @PathVariable("orderID") String orderID,
            @RequestBody PurchaseOrder updatedOrder
    ) {
        PurchaseOrder existingOrder = purchaseOrderService.getOrder(orderID);
        updatedOrder.setOrderId(orderID);
        PurchaseOrder updatedPurchaseOrder = purchaseOrderService.updateOrder(orderID, updatedOrder);
        return ResponseEntity.ok(updatedPurchaseOrder);
    }

    @GetMapping("/{employeeId}/orders")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ResponseEntity<Page<PurchaseOrder>> getOrdersByEmployeeId(
            @PathVariable(value = "employeeId") String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "searchField",defaultValue = "") String field,
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString
    ) {

        Page<PurchaseOrder> orders = purchaseOrderService.findOrderWithEmployeeID(page,size,field,seacrhString,employeeId);
        return ResponseEntity.ok(orders);
    }


    @PostMapping("/{employeeId}/trackings")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public WithHoldTracking createTracking(@PathVariable(value = "employeeId") String employeeId, @RequestBody WithHoldTracking track) {
        Employee employee = employeeService.getEmployee(employeeId);
        track.setEmployee(employee);
        return withHoldTrackingService.saveWithHoldTracking(track);
    }
    @PutMapping("/trackings/{trackingID}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ResponseEntity<WithHoldTracking> updateWithHoldTracking(
            @PathVariable("trackingID") String trackingID,
            @RequestBody WithHoldTracking updatedTracking
    ) {
        WithHoldTracking existingTracking = withHoldTrackingService.getWithHoldTrackingById(trackingID);
        updatedTracking.setTrackingId(trackingID);
        WithHoldTracking updatedWithHoldTracking = withHoldTrackingService.updateWithHoldTracking(trackingID, updatedTracking);
        return ResponseEntity.ok(updatedWithHoldTracking);
    }
    @GetMapping("/{employeeId}/trackings")
    public ResponseEntity<Page<WithHoldTracking>> getEmployeeWithHold(
            @PathVariable(value = "employeeId") String employeeId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "searchField",defaultValue = "") String field,
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString
    ) {
        Page<WithHoldTracking> trackings = withHoldTrackingService.findTrackingWithEmployeeID(page,size,field,seacrhString,employeeId);
        return ResponseEntity.ok(trackings);
    }

    @PostMapping("/{employeeId}/projects")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ProjectHistory createHistory(@PathVariable(value = "employeeId") String employeeId, @RequestBody ProjectHistory history){
        Employee employee = employeeService.getEmployee(employeeId);
        history.setEmployee(employee);
        return projectHistoryService.saveProjectHistory(history);
    }
    @PutMapping("/projects/{projectID}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ResponseEntity<ProjectHistory> updateHistory(
            @PathVariable("projectID") String projectID,
            @RequestBody ProjectHistory updateHistory
    ) {
        ProjectHistory existingProjectHistory = projectHistoryService.getProjectHistoryById(projectID);
        updateHistory.setProjectId(projectID);
        ProjectHistory updateProjectHistory = projectHistoryService.updateProjectHistory(projectID,updateHistory);
        return ResponseEntity.ok(updateProjectHistory);
    }
  
    @GetMapping("/{employeeId}/projects")
    @PreAuthorize("hasAnyRole('ADMIN', 'EMPLOYEE', 'SADMIN')")
    public ResponseEntity<Page<ProjectHistory>> getEmployeeProjectHistory(
            @PathVariable(value = "employeeId") String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "searchField",defaultValue = "") String field,
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString
    ) {
        Page<ProjectHistory> paginatedHistory = projectHistoryService.findProjectWithEmployeeID(page, size, field, seacrhString, employeeId);
        return ResponseEntity.ok(paginatedHistory);
    }

    @PostMapping("/{employeeId}/visa-details")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ResponseEntity<VisaDetails> createDetails(
            @PathVariable("employeeId") String employeeId,
            @RequestBody VisaDetails details
    ) {
        Employee employee = employeeService.getEmployee(employeeId);
        details.setEmployee(employee);
        VisaDetails savedDetails = visaDetailsService.saveDetails(details);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDetails);
    }

    @PutMapping("/visa-details/{visaID}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ResponseEntity<VisaDetails> updateVisaDetails(
            @PathVariable("visaID") String visaID,
            @RequestBody VisaDetails updateVisaDetails
    ){
        VisaDetails existingVisaDetails = visaDetailsService.getVisaDetailsById(visaID);
        updateVisaDetails.setVisaId(visaID);
        VisaDetails updateDetails = visaDetailsService.updateVisaDetails(visaID,updateVisaDetails);
        return ResponseEntity.ok(updateDetails);
    }
    @GetMapping("/{employeeId}/visa-details")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ResponseEntity<Page<VisaDetails>> getEmployeeVisaDetails(
            @PathVariable(value = "employeeId") String employeeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "searchField",defaultValue = "") String field,
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString
    ) {
        Page<VisaDetails> paginatedVisaDetails = visaDetailsService.findVisaDetailsWithEmployeeID(page, size, field, seacrhString,employeeId);
        return ResponseEntity.ok(paginatedVisaDetails);
    }

    // Weekly file upload endpoints
    @PostMapping(value = "/{employeeId}/uploadFiles", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'EMPLOYEE')")
    public ResponseEntity<String> uploadWeeklyFiles(
            @PathVariable String employeeId,
            @RequestParam("documents") MultipartFile file,
            @RequestParam("week") String week,
            @RequestParam(value = "description", required = false) String description) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("No file provided for upload");
            }
            employeeService.uploadWeeklyFiles(employeeId, week, file, description);
            return ResponseEntity.ok("File uploaded successfully");
        } catch (FileUploadException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload file: " + e.getMessage());
        }
    }

    @GetMapping("/{employeeId}/files/week/{week}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<List<String>> getWeeklyFiles(
            @PathVariable String employeeId,
            @PathVariable String week) {
        try {
            List<String> files = employeeService.getWeeklyFiles(employeeId, week);
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.emptyList());
        }
    }

    @GetMapping("/{employeeId}/files/week/{week}/{fileName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN', 'EMPLOYEE')")
    public ResponseEntity<byte[]> downloadWeeklyFile(
            @PathVariable String employeeId,
            @PathVariable String week,
            @PathVariable String fileName) {
        try {
            byte[] fileData = employeeService.downloadWeeklyFile(employeeId, week, fileName);
            
            // Determine Content-Type based on file extension
            String contentType = getContentType(fileName);
            
            return ResponseEntity.ok()
                    .header("Content-Type", contentType)
                    .header("Content-Disposition", "inline; filename=\"" + fileName + "\"")
                    .body(fileData);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }
    
    private String getContentType(String fileName) {
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lowerFileName.endsWith(".doc") || lowerFileName.endsWith(".docx")) {
            return "application/msword";
        } else if (lowerFileName.endsWith(".xls") || lowerFileName.endsWith(".xlsx")) {
            return "application/vnd.ms-excel";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFileName.endsWith(".png")) {
            return "image/png";
        } else if (lowerFileName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFileName.endsWith(".txt")) {
            return "text/plain";
        } else if (lowerFileName.endsWith(".html") || lowerFileName.endsWith(".htm")) {
            return "text/html";
        } else {
            return "application/octet-stream";
        }
    }

    @DeleteMapping("/{employeeId}/files/week/{week}/{fileName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'EMPLOYEE')")
    public ResponseEntity<String> deleteWeeklyFile(
            @PathVariable String employeeId,
            @PathVariable String week,
            @PathVariable String fileName) {
        try {
            employeeService.deleteWeeklyFile(employeeId, week, fileName);
            return ResponseEntity.ok("File deleted successfully");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("File not found or could not be deleted: " + e.getMessage());
        }
    }

    @GetMapping("/files/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> getAllWeeklyFiles(
            @RequestParam(name = "companyId", required = false) Integer companyId) {
        try {
            // Get current user
            String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
            
            // For GROUP_ADMIN, filter by selected company
            if (currentUser != null && currentUser.getRole() == Role.GROUP_ADMIN) {
                // Get GROUP_ADMIN's assigned companies
                List<UserCompanyRole> userRoles = userCompanyRoleRepository.findByUserId(currentUser.getId());
                List<Integer> assignedCompanyIds = userRoles.stream()
                        .map(UserCompanyRole::getCompanyId)
                        .toList();
                
                // If companyId is provided, validate it's one of their assigned companies
                if (companyId != null && assignedCompanyIds.contains(companyId)) {
                    // Valid company - use it for filtering
                } else if (companyId != null) {
                    // Invalid company - use first assigned as fallback
                    if (!assignedCompanyIds.isEmpty()) {
                        companyId = assignedCompanyIds.get(0);
                    } else {
                        // No companies assigned - return empty result
                        return ResponseEntity.ok(Collections.emptyList());
                    }
                } else if (!assignedCompanyIds.isEmpty()) {
                    // No companyId provided - use selected/default company
                    UserCompanyRole defaultRole = userRoles.stream()
                            .filter(role -> "true".equalsIgnoreCase(role.getDefaultCompany()))
                            .findFirst()
                            .orElse(userRoles.get(0));
                    companyId = defaultRole.getCompanyId();
                } else {
                    // No companies assigned - return empty result
                    return ResponseEntity.ok(Collections.emptyList());
                }
            }
            
            // For SADMIN, ignore companyId filter (show all)
            if (currentUser != null && currentUser.getRole() == Role.SADMIN) {
                companyId = null;
            }
            
            // For ADMIN, use their assigned company if no companyId provided
            if (currentUser != null && currentUser.getRole() == Role.ADMIN && companyId == null) {
                List<UserCompanyRole> adminRoles = userCompanyRoleRepository.findByUserId(currentUser.getId());
                if (!adminRoles.isEmpty()) {
                    UserCompanyRole defaultRole = adminRoles.stream()
                            .filter(role -> "true".equalsIgnoreCase(role.getDefaultCompany()))
                            .findFirst()
                            .orElse(adminRoles.get(0));
                    companyId = defaultRole.getCompanyId();
                }
            }
            
            List<Map<String, Object>> files = employeeService.getAllWeeklyFiles(companyId);
            return ResponseEntity.ok(files);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

}


