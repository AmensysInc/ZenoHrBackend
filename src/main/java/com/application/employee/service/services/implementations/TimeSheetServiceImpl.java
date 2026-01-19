package com.application.employee.service.services.implementations;

import com.application.employee.service.dto.TimeSheetDTO;
import com.application.employee.service.dto.TimeSheetRequestDTO;
import com.application.employee.service.dto.TimeSheetsDTO;
import com.application.employee.service.dto.UploadedFileDTO;
import com.application.employee.service.entities.TimeSheet;
import com.application.employee.service.entities.TimeSheetFile;
import com.application.employee.service.entities.TimeSheetMaster;
import com.application.employee.service.enums.TimeSheetsStatus;
import com.application.employee.service.repositories.TimeSheetFileRepo;
import com.application.employee.service.repositories.TimeSheetMasterRepo;
import com.application.employee.service.repositories.TimeSheetRepo;
import com.application.employee.service.services.EmployeeService;
import com.application.employee.service.services.ProjectHistoryService;
import com.application.employee.service.services.TimeSheetMasterService;
import com.application.employee.service.services.TimeSheetService;
import lombok.AllArgsConstructor;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TimeSheetServiceImpl implements TimeSheetService {

    private final TimeSheetRepo timeSheetRepo;
    private final TimeSheetMasterRepo timeSheetMasterRepo;
    private final TimeSheetFileRepo timeSheetFileRepo;
    private final EmployeeService employeeService;
    private final ProjectHistoryService projectHistoryService;
    private final TimeSheetMasterService timeSheetMasterService;
    private final String UploadPath = "D:\\My Drive\\New folder";

    @Override
    public void saveTimeSheet(TimeSheetDTO dto) {
        // 🧩 1️⃣ Validate inputs early
        if (dto.getEmployeeId() == null || dto.getEmployeeId().isBlank()) {
            throw new IllegalArgumentException("❌ EmployeeId cannot be null or blank");
        }
        if (dto.getProjectId() == null || dto.getProjectId().isBlank()) {
            throw new IllegalArgumentException("❌ ProjectId cannot be null or blank");
        }
        if (dto.getMonth() == null || dto.getYear() == null) {
            throw new IllegalArgumentException("❌ Month and Year are required");
        }

        // 🧠 2️⃣ Fetch or create the TimeSheetMaster
        TimeSheetMaster master = timeSheetMasterRepo
                .findByEmployeeEmployeeIDAndProjectHistoryProjectIdAndMonthAndYear(
                        dto.getEmployeeId(),
                        dto.getProjectId(),
                        dto.getMonth(),
                        dto.getYear()
                );

        if (master == null) {
            master = new TimeSheetMaster();
            master.setEmployee(employeeService.getEmployee(dto.getEmployeeId()));
            master.setProjectHistory(projectHistoryService.getProjectHistoryById(dto.getProjectId()));
            master.setMonth(dto.getMonth());
            master.setYear(dto.getYear());
            master = timeSheetMasterRepo.save(master);
        }

        // 🕵️‍♂️ 3️⃣ Check if TimeSheet already exists for this master and date
        Optional<TimeSheet> existingSheetOpt =
                timeSheetRepo.findByTimeSheetMasterMasterIdAndDate(master.getMasterId(), dto.getDate());

        TimeSheet timeSheet = existingSheetOpt.orElseGet(TimeSheet::new);

        // If this is an update, preserve the existing ID
        if (existingSheetOpt.isPresent()) {
            timeSheet.setSheetId(existingSheetOpt.get().getSheetId());
        } else if (dto.getSheetId() != null) {
            timeSheet.setSheetId(dto.getSheetId());
        }

        // 🧱 4️⃣ Map fields safely
        timeSheet.setTimeSheetMaster(master);
        timeSheet.setDate(dto.getDate());
        timeSheet.setRegularHours(dto.getRegularHours());
        timeSheet.setOverTimeHours(dto.getOverTimeHours());
        timeSheet.setStatus(dto.getStatus());
        timeSheet.setNotes(dto.getNotes());

        // Set FK fields for quick reference
        timeSheet.setEmpId(master.getEmployee().getEmployeeID());
        timeSheet.setProjectId(master.getProjectHistory().getProjectId());

        // 🧾 5️⃣ Save final record
        timeSheetRepo.save(timeSheet);

    }


    @Override
    public List<TimeSheet> getAllTimeSheets(TimeSheetRequestDTO timeSheetRequestDTO) {
        TimeSheetMaster timeSheetMaster = timeSheetMasterRepo.findByEmployeeEmployeeIDAndProjectHistoryProjectIdAndMonthAndYear(timeSheetRequestDTO.getEmployeeId(),
                timeSheetRequestDTO.getProjectId(),
                timeSheetRequestDTO.getMonth(),
                timeSheetRequestDTO.getYear());
        if(timeSheetMaster == null){
            timeSheetMaster = new TimeSheetMaster();
            timeSheetMaster.setMonth(timeSheetRequestDTO.getMonth());
            timeSheetMaster.setYear(timeSheetRequestDTO.getYear());
            timeSheetMaster.setEmployee(employeeService.getEmployee(timeSheetRequestDTO.getEmployeeId()));
            timeSheetMaster.setProjectHistory(projectHistoryService.getProjectHistoryById(timeSheetRequestDTO.getProjectId()));
            timeSheetMasterService.saveSheets(timeSheetMaster);
        }
        return timeSheetRepo.getAllByTimeSheetMasterMasterId(timeSheetMaster.getMasterId());
    }

    @Override
    public List<TimeSheet> getAllTimeSheet(TimeSheetsDTO timeSheetRequestDTO) {
        Integer month = timeSheetRequestDTO.getMonth();
        Integer year = timeSheetRequestDTO.getYear();

        List<TimeSheetMaster> timeSheetMasters = timeSheetMasterRepo.findAllByMonthAndYear(month, year);
        List<TimeSheet> allTimeSheets = new ArrayList<>();
        for (TimeSheetMaster timeSheetMaster : timeSheetMasters) {
            //need to check the loop is required
            List<TimeSheet> timeSheets = timeSheetRepo.getAllByTimeSheetMasterMasterId(timeSheetMaster.getMasterId());
            for (int i = 0; i < timeSheets.size(); i++ ){
                timeSheets.get(i).setEmpId(timeSheetMaster.getEmployee().getEmployeeID());
                timeSheets.get(i).setProjectId(timeSheetMaster.getProjectHistory().getProjectId());
            }
            allTimeSheets.addAll(timeSheets);
        }
        return allTimeSheets;
    }

    @Override
    public List<String> getAllTimeSheetStatus() {
        List<String> statusList = new ArrayList<>();
        for (TimeSheetsStatus status : TimeSheetsStatus.values()) {
            statusList.add(status.name());
        }

        return statusList;
    }

    @Override
    public void uploadFiles(String employeeID, String projectId, int year, String month, MultipartFile[] files)
            throws FileUploadException {

        Path targetFolder = Paths.get(UploadPath, employeeID, projectId, String.valueOf(year), month);

        try {
            if (!Files.exists(targetFolder)) {
                Files.createDirectories(targetFolder);
            }

            TimeSheetMaster master = timeSheetMasterRepo.findByEmployeeEmployeeIDAndProjectHistoryProjectIdAndMonthAndYear(
                    employeeID, projectId, monthToInt(month), year);

            if (master == null) {
                throw new FileNotFoundException("TimeSheetMaster not found for given parameters.");
            }

            for (MultipartFile file : files) {
                if (file.isEmpty() || file.getOriginalFilename() == null) {
                    throw new FileUploadException("One or more uploaded files are empty or invalid.");
                }

                String safeFileName = Paths.get(file.getOriginalFilename()).getFileName().toString(); // remove path info
                Path filePath = targetFolder.resolve(safeFileName);

                Files.write(filePath, file.getBytes());

                TimeSheetFile timeSheetFile = new TimeSheetFile();
                timeSheetFile.setFileName(safeFileName);
                timeSheetFile.setFilePath(filePath.toString());
                timeSheetFile.setUploadedAt(LocalDateTime.now());
                timeSheetFile.setTimeSheetMaster(master);

                timeSheetFileRepo.save(timeSheetFile);
            }

        } catch (IOException e) {
            // Now this shows the real issue in logs and response
            throw new FileUploadException("Failed to upload files: " + e.getMessage(), e);
        }
    }

    private int monthToInt(String monthName) {
        return java.time.Month.valueOf(monthName.toUpperCase()).getValue();
    }

    @Override
    public List<UploadedFileDTO> getUploadedFiles(String employeeID, String projectID, int year, String month) {
        List<UploadedFileDTO> uploadedFiles = new ArrayList<>();

        TimeSheetMaster master = timeSheetMasterRepo.findByEmployeeEmployeeIDAndProjectHistoryProjectIdAndMonthAndYear(
                employeeID, projectID, monthToInt(month), year);

        if (master != null) {
            List<TimeSheetFile> files = timeSheetFileRepo.findAllByTimeSheetMasterMasterId(master.getMasterId());

            for (TimeSheetFile file : files) {
                uploadedFiles.add(new UploadedFileDTO(file.getFileName(), file.getUploadedAt()));
            }
        }

        return uploadedFiles;
    }

    @Override
    public void deleteUploadedFile(String employeeID, String filename) {
        try {
            // Get all matching files (now handles multiple results)
            List<TimeSheetFile> files = timeSheetFileRepo.findByFileNameAndTimeSheetMasterEmployeeEmployeeID(filename, employeeID);

            if (files.isEmpty()) {
                throw new FileNotFoundException("File not found in DB: " + filename);
            }

            // Delete all matching files (or choose another strategy)
            for (TimeSheetFile fileEntity : files) {
                // Delete the file from the file system
                Path filePath = Paths.get(fileEntity.getFilePath());
                if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
                    Files.delete(filePath);
                }

                // Delete from DB
                timeSheetFileRepo.delete(fileEntity);
            }

        } catch (IOException e) {
            // Consider throwing a custom exception instead
            throw new RuntimeException("Failed to delete file: " + filename, e);
        }
    }

}
