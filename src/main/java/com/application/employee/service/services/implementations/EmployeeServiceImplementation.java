package com.application.employee.service.services.implementations;

import com.application.employee.service.Util.MapperUtil;
import com.application.employee.service.auth.AuthenticationService;
import com.application.employee.service.dto.EmployeeDTO;
import com.application.employee.service.dto.ProspectFileDTO;
import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.ProspectFile;
import com.application.employee.service.exceptions.ResourceNotFoundException;
import com.application.employee.service.repositories.EmployeeDetailsRespository;
import com.application.employee.service.repositories.EmployeeRespository;
import com.application.employee.service.repositories.ProspectFileRepository;
import com.application.employee.service.repositories.UserRepository;
import com.application.employee.service.repositories.UserCompanyRoleRepository;
import com.application.employee.service.entities.UserCompanyRole;
import com.application.employee.service.services.EmployeeService;
import com.application.employee.service.specifications.EmployeeSpecifications;
import com.application.employee.service.user.Role;
import com.application.employee.service.user.User;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EmployeeServiceImplementation implements EmployeeService {
    @Autowired
    private EmployeeRespository employeeRespository;
    @Autowired
    private EmployeeDetailsRespository employeeDetailsRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthenticationService authenticationService;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private ProspectFileRepository prospectFileRepository;
    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;
    @Value("${file.storage-location}")
    private String UploadPath;

    @Override
    public Employee saveEmployee(EmployeeDTO employeeDTO) {
        Optional<Employee> existingEmployee = employeeRespository.findByEmailID(employeeDTO.getEmailID());
        if (existingEmployee.isPresent()) {
            return null;
        }
        String randomEmployeeID = UUID.randomUUID().toString();
        employeeDTO.setEmployeeID(randomEmployeeID);
        Employee employee = MapperUtil.convertEmployeeDTO(employeeDTO);
        Employee savedEmployee = employeeRespository.save(employee);

        User newUser = new User();
        newUser.setId(employeeDTO.getEmployeeID());
        newUser.setFirstname(employeeDTO.getFirstName());
        newUser.setLastname(employeeDTO.getLastName());
        newUser.setEmail(employeeDTO.getEmailID());
        // Default to EMPLOYEE role if securityGroup is not specified
        newUser.setRole(employeeDTO.getSecurityGroup() != null ? employeeDTO.getSecurityGroup() : Role.EMPLOYEE);
        
        // Handle password - if provided, encode it; otherwise generate temp password
        if (employeeDTO.getPassword() != null && !employeeDTO.getPassword().isEmpty()) {
            newUser.setPassword(passwordEncoder.encode(employeeDTO.getPassword()));
        } else {
            var tempPassword = UUID.randomUUID().toString();
            newUser.setPassword(passwordEncoder.encode(tempPassword));
            newUser.setTempPassword(passwordEncoder.encode(tempPassword));
        }
        userRepository.save(newUser);
        
        // ✅ Automatically create UserCompanyRole if employee is assigned to a company
        if (employeeDTO.getCompanyId() != null) {
            UserCompanyRole userCompanyRole = new UserCompanyRole();
            userCompanyRole.setUserId(employeeDTO.getEmployeeID());
            userCompanyRole.setCompanyId(employeeDTO.getCompanyId());
            userCompanyRole.setRole(employeeDTO.getSecurityGroup() != null ? employeeDTO.getSecurityGroup().name() : Role.EMPLOYEE.name());
            userCompanyRole.setDefaultCompany("true");
            userCompanyRole.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
            
            // If this is the first company for this user, set as default
            List<UserCompanyRole> existingRoles = userCompanyRoleRepository.findByUserId(employeeDTO.getEmployeeID());
            if (existingRoles.isEmpty()) {
                userCompanyRole.setDefaultCompany("true");
            } else {
                // If user already has roles, set this as non-default
                userCompanyRole.setDefaultCompany("false");
            }
            
            userCompanyRoleRepository.save(userCompanyRole);
        }
        
        return savedEmployee;
    }


    @Override
    public List<Employee> getAllEmployee() {
        return employeeRespository.findAll();
    }

    @Override
    public List<Employee> getEmployeesByCompanyOrAll(Long company_id) {
        return employeeRespository.findByCompanyOrAll(company_id);
    }

    @Override
    public Employee getEmployee(String id) {
        // Use findByIdWithDetails to eagerly fetch employeeDetails
        return employeeRespository.findByIdWithDetails(id).orElseThrow(
                () -> new ResourceNotFoundException("Employee not found with given employeeID: " + id)
        );
    }
    @Override
    public Employee updateEmployee(String id, EmployeeDTO employeeDTO) {
        Employee existingEmployee = employeeRespository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Employee not found with given employeeID: " + id));
        User existingUser = userRepository.findById(id);
        if (existingUser == null) {
            existingUser = new User();
            existingUser.setId(id);
        }
        MapperUtil.updateEmployeeFromDTO(employeeDTO, existingEmployee);
        existingUser.setEmail(employeeDTO.getEmailID());
        // Default to EMPLOYEE role if securityGroup is not specified, or keep existing role if not provided
        if (employeeDTO.getSecurityGroup() != null) {
            existingUser.setRole(employeeDTO.getSecurityGroup());
        } else if (existingUser.getRole() == null) {
            existingUser.setRole(Role.EMPLOYEE);
        }
        existingUser.setFirstname(employeeDTO.getFirstName());
        existingUser.setLastname(employeeDTO.getLastName());
        userRepository.save(existingUser);
        
        // ✅ Update or create UserCompanyRole if company is assigned/changed
        if (employeeDTO.getCompanyId() != null) {
            List<UserCompanyRole> existingRoles = userCompanyRoleRepository.findByUserId(id);
            UserCompanyRole companyRole = existingRoles.stream()
                    .filter(role -> role.getCompanyId().equals(employeeDTO.getCompanyId()))
                    .findFirst()
                    .orElse(null);
            
            if (companyRole == null) {
                // Create new UserCompanyRole
                companyRole = new UserCompanyRole();
                companyRole.setUserId(id);
                companyRole.setCompanyId(employeeDTO.getCompanyId().intValue());
                companyRole.setRole(employeeDTO.getSecurityGroup() != null ? employeeDTO.getSecurityGroup().name() : Role.EMPLOYEE.name());
                
                // If this is the first company role, set as default
                if (existingRoles.isEmpty()) {
                    companyRole.setDefaultCompany("true");
                } else {
                    companyRole.setDefaultCompany("false");
                }
                
                companyRole.setCreatedAt(new java.sql.Date(System.currentTimeMillis()));
                userCompanyRoleRepository.save(companyRole);
            } else {
                // Update existing role if needed
                if (employeeDTO.getSecurityGroup() != null && !companyRole.getRole().equals(employeeDTO.getSecurityGroup().name())) {
                    companyRole.setRole(employeeDTO.getSecurityGroup().name());
                    userCompanyRoleRepository.save(companyRole);
                }
            }
        }
        
        // ✅ Update or create EmployeeDetails (contains visaStatus and other details)
        com.application.employee.service.entities.EmployeeDetails employeeDetails = existingEmployee.getEmployeeDetails();
        boolean isNewEmployeeDetails = false;
        if (employeeDetails == null) {
            employeeDetails = new com.application.employee.service.entities.EmployeeDetails();
            employeeDetails.setEmployeeDetailsID(id);
            employeeDetails.setEmployee(existingEmployee);
            isNewEmployeeDetails = true;
        }
        
        // Update visaStatus and other EmployeeDetails fields from DTO
        // Always update visaStatus if provided in DTO (allows setting to null/empty to clear it)
        if (employeeDTO.getVisaStatus() != null) {
            employeeDetails.setVisaStatus(employeeDTO.getVisaStatus());
        } else if (employeeDTO.getVisaStatus() == null && employeeDetails.getVisaStatus() != null) {
            // If DTO has visaStatus as null and existing has a value, allow clearing it
            // This handles the case where user explicitly clears the field
            employeeDetails.setVisaStatus(null);
        }
        if (employeeDTO.getFatherName() != null) {
            employeeDetails.setFatherName(employeeDTO.getFatherName());
        }
        if (employeeDTO.getSsn() != null) {
            employeeDetails.setSsn(employeeDTO.getSsn());
        }
        if (employeeDTO.getCurrentWorkLocation() != null) {
            employeeDetails.setCurrentWorkLocation(employeeDTO.getCurrentWorkLocation());
        }
        if (employeeDTO.getResidentialAddress() != null) {
            employeeDetails.setResidentialAddress(employeeDTO.getResidentialAddress());
        }
        if (employeeDTO.getHomeCountryAddress() != null) {
            employeeDetails.setHomeCountryAddress(employeeDTO.getHomeCountryAddress());
        }
        if (employeeDTO.getEmergencyContactDetails() != null) {
            employeeDetails.setEmergencyContactDetails(employeeDTO.getEmergencyContactDetails());
        }
        if (employeeDTO.getBachelorsDegree() != null) {
            employeeDetails.setBachelorsDegree(employeeDTO.getBachelorsDegree());
        }
        if (employeeDTO.getMastersDegree() != null) {
            employeeDetails.setMastersDegree(employeeDTO.getMastersDegree());
        }
        if (employeeDTO.getBankName() != null) {
            employeeDetails.setBankName(employeeDTO.getBankName());
        }
        if (employeeDTO.getAccType() != null) {
            employeeDetails.setAccType(employeeDTO.getAccType());
        }
        if (employeeDTO.getRoutingNumber() != null) {
            employeeDetails.setRoutingNumber(employeeDTO.getRoutingNumber());
        }
        if (employeeDTO.getAccNumber() != null) {
            employeeDetails.setAccNumber(employeeDTO.getAccNumber());
        }
        if (employeeDTO.getMaritalStatus() != null) {
            employeeDetails.setMaritalStatus(employeeDTO.getMaritalStatus());
        }
        if (employeeDTO.getItFilingState() != null) {
            employeeDetails.setItFilingState(employeeDTO.getItFilingState());
        }
        if (employeeDTO.getNeedInsurance() != null) {
            employeeDetails.setNeedInsurance(employeeDTO.getNeedInsurance());
        }
        if (employeeDTO.getStartDateWithAmensys() != null) {
            employeeDetails.setStartDateWithAmensys(employeeDTO.getStartDateWithAmensys());
        }
        
        // Save EmployeeDetails first (it's the owning side of the relationship)
        employeeDetailsRepository.save(employeeDetails);
        
        // Set the relationship on Employee and save
        existingEmployee.setEmployeeDetails(employeeDetails);
        
        return employeeRespository.save(existingEmployee);
    }

    @Override
    public void deleteEmployee(String id) {
        Employee existingEmployee = employeeRespository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Employee not found with given employeeID: " + id));
        User user = userRepository.findById(existingEmployee.getEmployeeID());

        if (user != null) {
            userRepository.delete(user);
        }

        employeeRespository.delete(existingEmployee);
    }

    @Override
    public void createProspectEmployee(Employee employee) throws IOException {
        String randomEmployeeID = UUID.randomUUID().toString();
        employee.setEmployeeID(randomEmployeeID);
        Employee savedEmployee;
        savedEmployee = employeeRespository.save(employee);

        User newUser = new User();
        newUser.setId(employee.getEmployeeID());
        newUser.setFirstname(employee.getFirstName());
        newUser.setLastname(employee.getLastName());
        newUser.setEmail(employee.getEmailID());
        newUser.setRole(Role.PROSPECT);
//        newUser.setPassword(passwordEncoder.encode(employee.getPassword()));
//        if (employee.getPassword() == null || employee.getPassword().isEmpty()) {
//            var tempPassword = UUID.randomUUID().toString();
//            newUser.setTempPassword(passwordEncoder.encode(tempPassword));
//            authenticationService.sendTemporaryPasswordEmail(employee.getEmailID(), tempPassword);
//        }
        userRepository.save(newUser);
    }

    @Override
    public void updateProspectEmployee(String id, Employee employee) {
        Employee existingEmployee = employeeRespository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Employee not found with given employeeID: " + id));

        existingEmployee.setFirstName(employee.getFirstName());
        existingEmployee.setLastName(employee.getLastName());
        existingEmployee.setEmailID(employee.getEmailID());
        existingEmployee.setDob(employee.getDob());
        existingEmployee.setPhoneNo(employee.getPhoneNo());
        existingEmployee.setClgOfGrad(employee.getClgOfGrad());
        existingEmployee.setOnBench(employee.getOnBench());
        existingEmployee.setMiddleName(employee.getMiddleName());
        existingEmployee.setEmployeeDetails(employee.getEmployeeDetails());

        employeeRespository.save(existingEmployee);
    }
/*
    @Override
    public void uploadProspectFiles(String employeeID, MultipartFile[] files) throws FileUploadException {
        if (files == null || files.length == 0) {
            throw new FileUploadException("No files provided for upload.");
        }

        try {
            // Log input values
            System.out.println("Uploading files for Employee ID: " + employeeID);
            System.out.println("Upload Path: " + UploadPath);

            // Create directory for this employee
            Path employeeUploadPath = Paths.get(UploadPath, employeeID);
            if (!Files.exists(employeeUploadPath)) {
                Files.createDirectories(employeeUploadPath);
                System.out.println("Created directory: " + employeeUploadPath);
            }

            // Save files
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), "File name is null");
                    Path filePath = employeeUploadPath.resolve(originalFilename);
                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    System.out.println("Saved file: " + filePath);
                } else {
                    System.out.println("Skipped empty file");
                }
            }

            // Update user role if exists
            Optional<User> existingUserOpt = Optional.ofNullable(userRepository.findById(employeeID));
            if (existingUserOpt.isPresent()) {
                User existingUser = existingUserOpt.get();
                existingUser.setRole(Role.EMPLOYEE);
                userRepository.save(existingUser);
                System.out.println("Updated role for employee: " + employeeID);
            } else {
                System.out.println("No user found with ID: " + employeeID);
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new FileUploadException("Failed to upload files for employee: " + employeeID, e);
        }
    }
*/
/*
    @Override
    public Page<Employee> findEmployeeWithPagination(int page, int size, String searchField, String searchString) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<Employee> spec = Specification.where(null);

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "firstName":
                    spec = spec.and(EmployeeSpecifications.firstNameContains(searchString));
                    break;
                case "middleName":
                    spec = spec.and(EmployeeSpecifications.middleNameEquals(searchString));
                    break;
                case "lastName":
                    spec = spec.and(EmployeeSpecifications.lastNameEquals(searchString));
                    break;
                case "emailID":
                    spec = spec.and(EmployeeSpecifications.emailIDEquals(searchString));
                    break;
                case "company":
                    spec = spec.and(EmployeeSpecifications.companyContains(searchString));
                    break;
                case "clgOfGrad":
                    spec = spec.and(EmployeeSpecifications.clgOfGradEquals(searchString));
                    break;
                case "phoneNo":
                    spec = spec.and(EmployeeSpecifications.phoneNoEquals(searchString));
                    break;
                case "onBench":
                    spec = spec.and(EmployeeSpecifications.onBenchEquals(searchString));
                    break;
            }
        }

        return employeeRespository.findAll(spec, pageable);
    }
*/

    @Override
    public Page<Employee> findEmployeeWithPagination(int page, int size, String searchField, String searchString) {
        return findEmployeeWithPagination(page, size, searchField, searchString, null);
    }

    @Override
    public Page<Employee> findEmployeeWithPagination(int page, int size, String searchField, String searchString, Long companyId) {
        return findEmployeeWithPagination(page, size, searchField, searchString, companyId, null);
    }

    @Override
    public Page<Employee> findEmployeeWithPagination(int page, int size, String searchField, String searchString, Long companyId, String reportingManagerId) {
        // Use unsorted pageable to avoid issues with String ID sorting
        Pageable pageable = PageRequest.of(page, size);

        Specification<Employee> spec = Specification.where(null);

        // Filter by company if companyId is provided
        if (companyId != null) {
            spec = spec.and(EmployeeSpecifications.companyIdEquals(companyId));
        }

        // Filter by reportingManagerId if provided
        if (reportingManagerId != null && !reportingManagerId.isEmpty()) {
            System.out.println("[EmployeeService] Filtering by reportingManagerId: " + reportingManagerId);
            spec = spec.and(EmployeeSpecifications.reportingManagerIdEquals(reportingManagerId));
        }

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "firstName":
                    spec = spec.and(EmployeeSpecifications.firstNameContains(searchString));
                    break;
                case "middleName":
                    spec = spec.and(EmployeeSpecifications.middleNameEquals(searchString));
                    break;
                case "lastName":
                    spec = spec.and(EmployeeSpecifications.lastNameEquals(searchString));
                    break;
                case "emailID":
                    spec = spec.and(EmployeeSpecifications.emailIDEquals(searchString));
                    break;
                case "company":
                    spec = spec.and(EmployeeSpecifications.companyContains(searchString));
                    break;
                case "clgOfGrad":
                    spec = spec.and(EmployeeSpecifications.clgOfGradEquals(searchString));
                    break;
                case "phoneNo":
                    spec = spec.and(EmployeeSpecifications.phoneNoEquals(searchString));
                    break;
                case "onBench":
                    spec = spec.and(EmployeeSpecifications.onBenchEquals(searchString));
                    break;
            }
        }

        return employeeRespository.findAll(spec, pageable);
    }

    @Override
    public List<Employee> getEmployeesBySecurityGroup(Role securityGroup) {
        return employeeRespository.findBySecurityGroup(securityGroup);
    }

    @Override
    public List<String> getProspectEmployeeFiles(String employeeID) throws IOException {
        Path employeeDir = Paths.get(UploadPath, employeeID);
        if (!Files.exists(employeeDir)) {
            return List.of(); // No files found
        }

        try (var paths = Files.list(employeeDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .toList();
        }
    }

    @Override
    public byte[] downloadProspectEmployeeFile(String employeeID, String fileName) throws IOException {
        Path filePath = Paths.get(UploadPath, employeeID, fileName);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileName);
        }
        return Files.readAllBytes(filePath);
    }
    @Override
    public Map<String, List<String>> getAllEmployeesWithFiles() throws IOException {
        Map<String, List<String>> employeeFilesMap = new HashMap<>();
        List<Employee> employees = employeeRespository.findAll();

        for (Employee employee : employees) {
            String empId = employee.getEmployeeID();
            List<String> files = getProspectEmployeeFiles(empId);
            employeeFilesMap.put(empId, files);
        }
        return employeeFilesMap;
    }
    @Override
    public void uploadProspectFiles(String employeeID, MultipartFile[] files) throws FileUploadException {
        if (files == null || files.length == 0) {
            throw new FileUploadException("No files provided for upload.");
        }

        try {
            Path employeeUploadPath = Paths.get(UploadPath, employeeID);
            if (!Files.exists(employeeUploadPath)) {
                Files.createDirectories(employeeUploadPath);
            }

            Employee employee = employeeRespository.findById(employeeID)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeID));

            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), "File name is null");
                    Path filePath = employeeUploadPath.resolve(originalFilename);
                    Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    // Save metadata
                    ProspectFile prospectFile = new ProspectFile();
                    prospectFile.setFileName(originalFilename);
                    prospectFile.setUploadTime(LocalDateTime.now());
                    prospectFile.setEmployee(employee);

                    prospectFileRepository.save(prospectFile);
                }
            }

            Optional<User> existingUserOpt = Optional.ofNullable(userRepository.findById(employeeID));
            if (existingUserOpt.isPresent()) {
                User existingUser = existingUserOpt.get();
                existingUser.setRole(Role.EMPLOYEE);
                userRepository.save(existingUser);
            }

        } catch (IOException e) {
            throw new FileUploadException("Failed to upload files for employee: " + employeeID, e);
        }
    }

    @Override
    public List<ProspectFileDTO> getAllProspectFiles() {
        List<ProspectFile> files = prospectFileRepository.findAll();

        return files.stream().map(file -> {
            ProspectFileDTO dto = new ProspectFileDTO();
            dto.setFileName(file.getFileName());
            dto.setUploadTime(file.getUploadTime());
            dto.setUploadedBy(file.getUploadedBy());
            dto.setEmployeeId(file.getEmployee().getEmployeeID());
            dto.setFirstName(file.getEmployee().getFirstName());
            dto.setLastName(file.getEmployee().getLastName());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ProspectFileDTO> searchProspectFiles(String search) {
        List<ProspectFile> files = prospectFileRepository.findAll();

        return files.stream()
                .filter(file -> {
                    String fullName = (file.getEmployee().getFirstName() + " " + file.getEmployee().getLastName()).toLowerCase();
                    return (file.getFileName() != null && file.getFileName().toLowerCase().contains(search.toLowerCase())) ||
                            (file.getEmployee().getEmployeeID() != null && file.getEmployee().getEmployeeID().toLowerCase().contains(search.toLowerCase())) ||
                            (fullName.contains(search.toLowerCase()));
                })
                .map(file -> {
                    ProspectFileDTO dto = new ProspectFileDTO();
                    dto.setFileName(file.getFileName());
                    dto.setUploadTime(file.getUploadTime());
                    dto.setUploadedBy(file.getUploadedBy());
                    dto.setEmployeeId(file.getEmployee().getEmployeeID());
                    dto.setFirstName(file.getEmployee().getFirstName());
                    dto.setLastName(file.getEmployee().getLastName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void deleteProspectFile(String employeeID, String fileName) throws IOException {
        Path filePath = Paths.get(UploadPath, employeeID, fileName);

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileName);
        }

        // Delete from file system
        Files.delete(filePath);

        // Delete metadata from DB
        List<ProspectFile> files = prospectFileRepository.findByEmployee_EmployeeID(employeeID)
                .stream()
                .filter(f -> f.getFileName().equals(fileName))
                .toList();

        for (ProspectFile f : files) {
            prospectFileRepository.delete(f);
        }
    }

    // Weekly file upload methods
    @Override
    public void uploadWeeklyFiles(String employeeId, String week, MultipartFile file, String description) throws FileUploadException {
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("No file provided for upload.");
        }

        try {
            // Create directory structure: UploadPath/{employeeId}/week/{week}/
            Path weekDir = Paths.get(UploadPath, employeeId, "week", week);
            if (!Files.exists(weekDir)) {
                Files.createDirectories(weekDir);
            }

            String originalFilename = Objects.requireNonNull(file.getOriginalFilename(), "File name is null");
            // Sanitize filename to prevent path traversal
            String safeFileName = Paths.get(originalFilename).getFileName().toString();
            Path filePath = weekDir.resolve(safeFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            throw new FileUploadException("Failed to upload weekly file for employee: " + employeeId + ". Error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new FileUploadException("Unexpected error uploading file: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> getWeeklyFiles(String employeeId, String week) throws IOException {
        Path weekDir = Paths.get(UploadPath, employeeId, "week", week);
        if (!Files.exists(weekDir)) {
            return List.of(); // No files found
        }

        try (var paths = Files.list(weekDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .toList();
        }
    }

    @Override
    public byte[] downloadWeeklyFile(String employeeId, String week, String fileName) throws IOException {
        Path filePath = Paths.get(UploadPath, employeeId, "week", week, fileName);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileName);
        }
        return Files.readAllBytes(filePath);
    }

    @Override
    public void deleteWeeklyFile(String employeeId, String week, String fileName) throws IOException {
        Path filePath = Paths.get(UploadPath, employeeId, "week", week, fileName);
        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + fileName);
        }
        Files.delete(filePath);
    }

    @Override
    public List<Map<String, Object>> getAllWeeklyFiles(Integer companyId) throws IOException {
        return getAllWeeklyFiles(companyId, null);
    }

    @Override
    public List<Map<String, Object>> getAllWeeklyFiles(Integer companyId, String reportingManagerId) throws IOException {
        List<Map<String, Object>> allFiles = new ArrayList<>();
        List<Employee> employees;
        
        // Filter employees by company if companyId is provided
        if (companyId != null) {
            employees = employeeRespository.findByCompanyCompanyId(companyId.longValue());
        } else {
            employees = employeeRespository.findAll();
        }
        
        // Filter by reportingManagerId if provided
        if (reportingManagerId != null && !reportingManagerId.isEmpty()) {
            employees = employees.stream()
                    .filter(emp -> reportingManagerId.equals(emp.getReportingManagerId()))
                    .collect(java.util.stream.Collectors.toList());
        }

        for (Employee employee : employees) {
            String empId = employee.getEmployeeID();
            Path employeeWeekDir = Paths.get(UploadPath, empId, "week");
            
            if (Files.exists(employeeWeekDir)) {
                try (var weekDirs = Files.list(employeeWeekDir)) {
                    weekDirs.filter(Files::isDirectory).forEach(weekDir -> {
                        String week = weekDir.getFileName().toString();
                        try (var files = Files.list(weekDir)) {
                            files.filter(Files::isRegularFile).forEach(file -> {
                                Map<String, Object> fileData = new HashMap<>();
                                fileData.put("employeeId", empId);
                                fileData.put("employeeName", employee.getFirstName() + " " + employee.getLastName());
                                fileData.put("employeeEmail", employee.getEmailID());
                                fileData.put("week", week);
                                fileData.put("fileName", file.getFileName().toString());
                                fileData.put("uploadTime", LocalDateTime.ofInstant(
                                    java.nio.file.attribute.FileTime.fromMillis(
                                        file.toFile().lastModified()
                                    ).toInstant(), 
                                    java.time.ZoneId.systemDefault()
                                ));
                                allFiles.add(fileData);
                            });
                        } catch (IOException e) {
                            // Log error but continue
                        }
                    });
                } catch (IOException e) {
                    // Log error but continue
                }
            }
        }

        return allFiles;
    }
}