package com.application.employee.service.services.implementations;

import com.application.employee.service.dto.AddressVerificationDTO;
import com.application.employee.service.dto.AddressVerificationRequest;
import com.application.employee.service.dto.AddressVerificationUpdateRequest;
import com.application.employee.service.entities.AddressVerification;
import com.application.employee.service.entities.Employee;
import com.application.employee.service.exceptions.ResourceNotFoundException;
import com.application.employee.service.repositories.AddressVerificationRepository;
import com.application.employee.service.services.AddressVerificationService;
import com.application.employee.service.services.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AddressVerificationServiceImpl implements AddressVerificationService {

    @Autowired
    private AddressVerificationRepository addressVerificationRepository;

    @Autowired
    private EmployeeService employeeService;

    @Override
    public AddressVerificationDTO createOrUpdateAddressVerification(String employeeId, AddressVerificationRequest request) {
        Employee employee = employeeService.getEmployee(employeeId);
        
        Optional<AddressVerification> existingOpt = addressVerificationRepository.findByEmployee(employee);
        AddressVerification addressVerification;

        if (existingOpt.isPresent()) {
            addressVerification = existingOpt.get();
            if (request.getHomeAddress() != null) {
                addressVerification.setHomeAddress(request.getHomeAddress());
            }
            if (request.getWorkAddress() != null) {
                addressVerification.setWorkAddress(request.getWorkAddress());
            }
            if (request.getIsWorking() != null) {
                addressVerification.setIsWorking(request.getIsWorking());
            }
            if (request.getNotes() != null) {
                addressVerification.setNotes(request.getNotes());
            }
        } else {
            addressVerification = new AddressVerification();
            addressVerification.setEmployee(employee);
            addressVerification.setHomeAddress(request.getHomeAddress());
            addressVerification.setWorkAddress(request.getWorkAddress());
            addressVerification.setIsWorking(request.getIsWorking() != null ? request.getIsWorking() : false);
            addressVerification.setHomeAddressVerified(false);
            addressVerification.setWorkAddressVerified(false);
            addressVerification.setNotes(request.getNotes());
        }

        AddressVerification saved = addressVerificationRepository.save(addressVerification);
        return convertToDTO(saved);
    }

    @Override
    public AddressVerificationDTO getAddressVerificationByEmployeeId(String employeeId) {
        Optional<AddressVerification> verificationOpt = addressVerificationRepository.findByEmployeeEmployeeID(employeeId);
        if (verificationOpt.isPresent()) {
            return convertToDTO(verificationOpt.get());
        }
        // Return empty DTO if not found
        AddressVerificationDTO dto = new AddressVerificationDTO();
        dto.setEmployeeId(employeeId);
        Employee employee = employeeService.getEmployee(employeeId);
        dto.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
        dto.setEmployeeEmail(employee.getEmailID());
        return dto;
    }

    @Override
    public AddressVerificationDTO updateAddressVerification(String employeeId, AddressVerificationUpdateRequest request) {
        Employee employee = employeeService.getEmployee(employeeId);
        AddressVerification addressVerification = addressVerificationRepository.findByEmployee(employee)
                .orElseThrow(() -> new ResourceNotFoundException("Address verification not found for employee: " + employeeId));

        if (request.getHomeAddress() != null) {
            addressVerification.setHomeAddress(request.getHomeAddress());
        }
        if (request.getWorkAddress() != null) {
            addressVerification.setWorkAddress(request.getWorkAddress());
        }
        if (request.getHomeAddressVerified() != null) {
            addressVerification.setHomeAddressVerified(request.getHomeAddressVerified());
            if (request.getHomeAddressVerified()) {
                addressVerification.setHomeAddressVerifiedDate(LocalDate.now());
            }
        }
        if (request.getWorkAddressVerified() != null) {
            addressVerification.setWorkAddressVerified(request.getWorkAddressVerified());
            if (request.getWorkAddressVerified()) {
                addressVerification.setWorkAddressVerifiedDate(LocalDate.now());
            }
        }
        if (request.getIsWorking() != null) {
            addressVerification.setIsWorking(request.getIsWorking());
        }
        if (request.getVerifiedBy() != null) {
            addressVerification.setVerifiedBy(request.getVerifiedBy());
        }
        if (request.getNotes() != null) {
            addressVerification.setNotes(request.getNotes());
        }

        AddressVerification saved = addressVerificationRepository.save(addressVerification);
        return convertToDTO(saved);
    }

    @Override
    public AddressVerificationDTO verifyHomeAddress(String employeeId, String verifiedBy) {
        Employee employee = employeeService.getEmployee(employeeId);
        AddressVerification addressVerification = addressVerificationRepository.findByEmployee(employee)
                .orElseThrow(() -> new ResourceNotFoundException("Address verification not found for employee: " + employeeId));

        addressVerification.setHomeAddressVerified(true);
        addressVerification.setHomeAddressVerifiedDate(LocalDate.now());
        addressVerification.setVerifiedBy(verifiedBy);

        AddressVerification saved = addressVerificationRepository.save(addressVerification);
        return convertToDTO(saved);
    }

    @Override
    public AddressVerificationDTO verifyWorkAddress(String employeeId, String verifiedBy) {
        Employee employee = employeeService.getEmployee(employeeId);
        AddressVerification addressVerification = addressVerificationRepository.findByEmployee(employee)
                .orElseThrow(() -> new ResourceNotFoundException("Address verification not found for employee: " + employeeId));

        addressVerification.setWorkAddressVerified(true);
        addressVerification.setWorkAddressVerifiedDate(LocalDate.now());
        addressVerification.setVerifiedBy(verifiedBy);

        AddressVerification saved = addressVerificationRepository.save(addressVerification);
        return convertToDTO(saved);
    }

    @Override
    public List<AddressVerificationDTO> getAllAddressVerifications() {
        return addressVerificationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private AddressVerificationDTO convertToDTO(AddressVerification verification) {
        AddressVerificationDTO dto = new AddressVerificationDTO();
        dto.setId(verification.getId());
        if (verification.getEmployee() != null) {
            dto.setEmployeeId(verification.getEmployee().getEmployeeID());
            dto.setEmployeeName(verification.getEmployee().getFirstName() + " " + verification.getEmployee().getLastName());
            dto.setEmployeeEmail(verification.getEmployee().getEmailID());
        }
        dto.setHomeAddress(verification.getHomeAddress());
        dto.setHomeAddressVerified(verification.getHomeAddressVerified());
        dto.setHomeAddressVerifiedDate(verification.getHomeAddressVerifiedDate());
        dto.setWorkAddress(verification.getWorkAddress());
        dto.setWorkAddressVerified(verification.getWorkAddressVerified());
        dto.setWorkAddressVerifiedDate(verification.getWorkAddressVerifiedDate());
        dto.setIsWorking(verification.getIsWorking());
        dto.setVerifiedBy(verification.getVerifiedBy());
        dto.setNotes(verification.getNotes());
        return dto;
    }
}

