package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.Paystub;
import com.application.employee.service.repositories.EmployeeRespository;
import com.application.employee.service.repositories.PaystubRepository;
import com.application.employee.service.services.PaystubService;
import com.application.employee.service.exceptions.ResourceNotFoundException;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaystubServiceImpl implements PaystubService {

    @Autowired
    private PaystubRepository paystubRepository;

    @Autowired
    private EmployeeRespository employeeRespository;

    @Value("${file.storage-location}")
    private String uploadPath;

    @Override
    public Paystub uploadPaystub(String employeeId, MultipartFile file, Integer year, LocalDate payPeriodStart,
                                 LocalDate payPeriodEnd, BigDecimal grossPay, BigDecimal netPay,
                                 String uploadedBy) throws FileUploadException, IOException {
        
        if (file == null || file.isEmpty()) {
            throw new FileUploadException("File cannot be empty");
        }

        Employee employee = employeeRespository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + employeeId));

        // Create directory structure: uploadPath/paystubs/employeeId/
        Path paystubDir = Paths.get(uploadPath, "paystubs", employeeId);
        Files.createDirectories(paystubDir);

        // Save file
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new FileUploadException("File name cannot be empty");
        }

        // Generate unique filename with timestamp to avoid conflicts
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileExtension = "";
        int lastDotIndex = originalFilename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            fileExtension = originalFilename.substring(lastDotIndex);
        }
        String uniqueFileName = timestamp + "_" + originalFilename;
        
        Path filePath = paystubDir.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create Paystub entity
        Paystub paystub = new Paystub();
        paystub.setEmployee(employee);
        paystub.setFileName(originalFilename);
        paystub.setFilePath(filePath.toString());
        paystub.setPayPeriodStart(payPeriodStart);
        paystub.setPayPeriodEnd(payPeriodEnd);
        paystub.setGrossPay(grossPay);
        paystub.setNetPay(netPay);
        paystub.setUploadedAt(LocalDateTime.now());
        paystub.setUploadedBy(uploadedBy);
        paystub.setMonth(payPeriodStart.getMonthValue());
        paystub.setYear(year); // Use the provided year instead of deriving from date

        return paystubRepository.save(paystub);
    }

    @Override
    public List<Paystub> getPaystubsByEmployee(String employeeId) {
        return paystubRepository.findByEmployeeEmployeeIDOrderByPayPeriodStartDesc(employeeId);
    }

    @Override
    public List<Paystub> getAllPaystubs() {
        return paystubRepository.findAllWithEmployee();
    }

    @Override
    public Paystub getPaystubById(Long id) {
        return paystubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paystub not found with ID: " + id));
    }

    @Override
    public byte[] downloadPaystub(Long id) throws IOException {
        Paystub paystub = paystubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paystub not found with ID: " + id));

        Path filePath = Paths.get(paystub.getFilePath());
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Paystub file not found at path: " + paystub.getFilePath());
        }

        return Files.readAllBytes(filePath);
    }

    @Override
    public void deletePaystub(Long id) {
        Paystub paystub = paystubRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paystub not found with ID: " + id));

        // Delete file from filesystem
        try {
            Path filePath = Paths.get(paystub.getFilePath());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        } catch (IOException e) {
            // Log error but continue with database deletion
            System.err.println("Failed to delete paystub file: " + e.getMessage());
        }

        // Delete from database
        paystubRepository.delete(paystub);
    }
}

