package com.application.employee.service.services;

import com.application.employee.service.dto.TimeSheetDTO;
import com.application.employee.service.dto.TimeSheetRequestDTO;
import com.application.employee.service.dto.TimeSheetsDTO;
import com.application.employee.service.dto.UploadedFileDTO;
import com.application.employee.service.entities.TimeSheet;
import com.application.employee.service.enums.TimeSheetsStatus;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface TimeSheetService {
    public void saveTimeSheet(TimeSheetDTO timeSheet);

    List<TimeSheet> getAllTimeSheets(TimeSheetRequestDTO timeSheetRequestDTO);

    List<String> getAllTimeSheetStatus();

    void uploadFiles(String employeeID, String projectId, int year, String month,MultipartFile[] files) throws FileUploadException;

    public List<UploadedFileDTO> getUploadedFiles(String employeeID, String projectID, int year, String month);

    void deleteUploadedFile(String employeeID, String filename);

    List<TimeSheet> getAllTimeSheet(TimeSheetsDTO timeSheetRequestDTO);
}
