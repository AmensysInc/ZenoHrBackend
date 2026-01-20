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
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
    public ResponseEntity<String> createEmployee(@RequestBody EmployeeDTO employeeDTO) {
        // ✅ If Admin creates employee without company, auto-assign to Admin's company
        // Get current user from SecurityContext
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByEmail(currentUserEmail).orElse(null);
        
        if (currentUser != null && currentUser.getRole() == Role.ADMIN) {
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
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString
    ) {
        Page<Employee> employees = employeeService.findEmployeeWithPagination(page, size, field, seacrhString);
        return ResponseEntity.ok(employees);
    }

    @PutMapping("/{employeeID}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SADMIN')")
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

}


