package com.application.employee.service.services;

import com.application.employee.service.entities.Paystub;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface PaystubService {
    Paystub uploadPaystub(String employeeId, MultipartFile file, Integer year, LocalDate payPeriodStart, 
                         LocalDate payPeriodEnd, BigDecimal grossPay, BigDecimal netPay, 
                         String uploadedBy) throws FileUploadException, IOException;
    
    List<Paystub> getPaystubsByEmployee(String employeeId);
    
    List<Paystub> getAllPaystubs();
    
    Paystub getPaystubById(Long id);
    
    byte[] downloadPaystub(Long id) throws IOException;
    
    void deletePaystub(Long id);
}

