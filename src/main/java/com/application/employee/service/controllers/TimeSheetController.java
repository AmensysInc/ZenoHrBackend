package com.application.employee.service.controllers;

import com.application.employee.service.dto.TimeSheetDTO;
import com.application.employee.service.dto.TimeSheetRequestDTO;
import com.application.employee.service.dto.TimeSheetsDTO;
import com.application.employee.service.dto.UploadedFileDTO;
import com.application.employee.service.entities.TimeSheet;
import com.application.employee.service.entities.TimeSheetMaster;
import com.application.employee.service.services.EmployeeService;
import com.application.employee.service.services.ProjectHistoryService;
import com.application.employee.service.services.TimeSheetMasterService;
import com.application.employee.service.services.TimeSheetService;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.core.io.Resource;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/timeSheets")
@AllArgsConstructor
public class TimeSheetController {

    private final EmployeeService employeeService;
    private final TimeSheetMasterService timeSheetMasterService;
    private final ProjectHistoryService projectHistoryService;
    private final TimeSheetService timeSheetService;

    private final String UploadPath = "D:\\My Drive\\New folder";

    @PostMapping("/getAllTimeSheetsByMonthYear")
    public ResponseEntity<List<TimeSheet>> getAllTimeSheetsByMonthYear(
            @RequestBody TimeSheetsDTO timeSheetsDTO
    ) {
        List<TimeSheet> timeSheets = timeSheetService.getAllTimeSheet(timeSheetsDTO);
        return ResponseEntity.ok(timeSheets);
    }

    @PostMapping("/create")
    public String createTimeSheetMasterForEmployeeAndProject(
            @RequestParam(value = "employeeId") String employeeId,
            @RequestParam(value = "projectId") String projectId,
            @RequestBody TimeSheetMaster timeSheetMaster
    ) {
        timeSheetMaster.setEmployee(employeeService.getEmployee(employeeId));
        timeSheetMaster.setProjectHistory(projectHistoryService.getProjectHistoryById(projectId));

        timeSheetMasterService.saveSheets(timeSheetMaster);

        return "TimeSheetMaster successfully submitted!";
    }

    @GetMapping("/all")
    public List<TimeSheet> getAllTimeSheet(@RequestBody TimeSheetsDTO timeSheetRequestDTO) {
            return timeSheetService.getAllTimeSheet(timeSheetRequestDTO);
    }

    @GetMapping
    public TimeSheetMaster getTimeSheetsByEmployeeAndMonthYear(
            @RequestParam(value = "masterId") Integer masterId
    ){
        return timeSheetMasterService.getTimeSheetmaster(masterId);
    }

//    @PostMapping("/createTimeSheet")
//    public String createTimeSheet(
//            @RequestBody ArrayList<TimeSheetDTO> timeSheetDTOs
//    ){
//        timeSheetDTOs.stream().forEach( timesheet ->
//            timeSheetService.saveTimeSheet(timesheet));
//
//        return "Time Sheet created successfully";
//    }

    @PostMapping("/createTimeSheet")
    public ResponseEntity<String> createTimeSheet(@RequestBody ArrayList<TimeSheetDTO> timeSheetDTOs) {
        List<String> issues = new ArrayList<>();

        for (TimeSheetDTO dto : timeSheetDTOs) {
            System.out.printf("Incoming DTO -> EmployeeId: %s, ProjectHistoryId: %s%n",
                    dto.getEmployeeId(), dto.getProjectId());

            if (dto.getEmployeeId() == null || dto.getEmployeeId().isBlank()) {
                issues.add("❌ Skipped: EmployeeId missing for ProjectHistoryId " + dto.getProjectId());
                continue;
            }

            if (dto.getProjectId() == null || dto.getProjectId().isBlank()) {
                issues.add("❌ Skipped: ProjectId missing for EmployeeId " + dto.getEmployeeId());
                continue;
            }

            try {
                timeSheetService.saveTimeSheet(dto);
            } catch (Exception e) {
                issues.add("❌ Failed for EmployeeId " + dto.getEmployeeId() + ": " + e.getMessage());
            }
        }

        if (issues.isEmpty()) {
            return ResponseEntity.ok("✅ All TimeSheets created successfully");
        }

        return ResponseEntity.badRequest().body(String.join("\n", issues));
    }

    @PostMapping("/createSheet")
    public ResponseEntity<String> createSheet(
            @RequestBody ArrayList<TimeSheetDTO> timeSheetDTOs
    ) {
        List<String> results = new ArrayList<>();
        try {
            timeSheetDTOs.forEach(timesheet -> {
                try {
                    timeSheetService.saveTimeSheet(timesheet);
                    results.add("Time Sheet created successfully for: " + timesheet.toString());
                } catch (Exception e) {
                    results.add("Failed to create Time Sheet for: " + timesheet.toString() + " - " + e.getMessage());
                }
            });
            return ResponseEntity.ok(results.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to create Time Sheets: " + e.getMessage());
        }
    }

    @PostMapping("/getAllTimeSheets")
    public List<TimeSheet> getAllTimeSheets(@RequestBody TimeSheetRequestDTO timeSheetRequestDTO){
        return timeSheetService.getAllTimeSheets(timeSheetRequestDTO);
    }

    @GetMapping("/getAllStatus")
    public List<String> getAllStatus(){
        return timeSheetService.getAllTimeSheetStatus();
    }

    @PostMapping("/uploadfiles/{employeeID}/{projectId}/{year}/{month}")
    public ResponseEntity<String> updateTimesheetsFiles(
            @PathVariable String employeeID,
            @PathVariable String projectId,
            @PathVariable int year,
            @PathVariable String month,
            @RequestParam("documents") MultipartFile[] files) {

        try {
            // Optional: Normalize the month string
            month = month.substring(0, 1).toUpperCase() + month.substring(1).toLowerCase();

            timeSheetService.uploadFiles(employeeID, projectId, year, month, files);
            return ResponseEntity.ok("TimeSheets files uploaded successfully for project: " + projectId);
        } catch (FileUploadException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to upload files: " + e.getMessage());
        }
    }


    @GetMapping("/getUploadedFiles/{employeeID}/{projectID}/{year}/{month}")
    public ResponseEntity<List<UploadedFileDTO>> getUploadedFiles(
            @PathVariable String employeeID,
            @PathVariable String projectID,
            @PathVariable int year,
            @PathVariable String month) {

        month = month.substring(0, 1).toUpperCase() + month.substring(1).toLowerCase();

        List<UploadedFileDTO> files = timeSheetService.getUploadedFiles(employeeID, projectID, year, month);
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/deleteUploadedFile/{employeeID}/{filename}")
    public ResponseEntity<?> deleteUploadedFile(
            @PathVariable String employeeID,
            @PathVariable String filename) {
        try {
            timeSheetService.deleteUploadedFile(employeeID, filename);
            return ResponseEntity.ok("Uploaded file '" + filename + "' deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to delete file: " + e.getMessage());
        }
    }

    @GetMapping("/downloadFile/{employeeID}/{projectID}/{year}/{month}/{filename}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String employeeID,
            @PathVariable String projectID,
            @PathVariable int year,
            @PathVariable String month,
            @PathVariable String filename) {

        // Normalize month format to match folder structure
        month = month.substring(0, 1).toUpperCase() + month.substring(1).toLowerCase();

        // Build full path to the file
        Path filePath = Paths.get(UploadPath, employeeID, projectID, String.valueOf(year), month, filename);

        try {
            // Load the file as a resource
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(filePath));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM); // safer for all file types
            headers.setContentDispositionFormData("attachment", filename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }

    @GetMapping("/templates/download")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'HR_MANAGER')")
    public ResponseEntity<Resource> downloadTimesheetTemplate(
            @RequestParam(required = false, defaultValue = "weekly") String templateType) {
        try {
            // Template file path - you can customize this based on your template storage
            String templateFileName;
            switch (templateType.toLowerCase()) {
                case "weekly":
                    templateFileName = "timesheet_weekly_template.xlsx";
                    break;
                case "monthly":
                    templateFileName = "timesheet_monthly_template.xlsx";
                    break;
                default:
                    templateFileName = "timesheet_weekly_template.xlsx";
            }

            // Build path to template file (adjust path as needed)
            Path templatePath = Paths.get(UploadPath, "templates", templateFileName);

            // If template doesn't exist, create a basic one or return error
            if (!Files.exists(templatePath)) {
                // You might want to generate a template programmatically here
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(templatePath));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", templateFileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(null);
        }
    }

    @GetMapping("/templates/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'HR_MANAGER', 'EMPLOYEE')")
    public ResponseEntity<List<Map<String, String>>> getAvailableTemplates() {
        List<Map<String, String>> templates = new ArrayList<>();
        
        Map<String, String> weekly = new HashMap<>();
        weekly.put("type", "weekly");
        weekly.put("name", "Weekly Timesheet Template");
        weekly.put("description", "Template for weekly timesheet submission");
        templates.add(weekly);

        Map<String, String> monthly = new HashMap<>();
        monthly.put("type", "monthly");
        monthly.put("name", "Monthly Timesheet Template");
        monthly.put("description", "Template for monthly timesheet submission");
        templates.add(monthly);

        return ResponseEntity.ok(templates);
    }
}
