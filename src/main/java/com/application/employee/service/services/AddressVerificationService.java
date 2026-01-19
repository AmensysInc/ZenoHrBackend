package com.application.employee.service.services;

import com.application.employee.service.dto.AddressVerificationDTO;
import com.application.employee.service.dto.AddressVerificationRequest;
import com.application.employee.service.dto.AddressVerificationUpdateRequest;

import java.util.List;

public interface AddressVerificationService {
    AddressVerificationDTO createOrUpdateAddressVerification(String employeeId, AddressVerificationRequest request);
    AddressVerificationDTO getAddressVerificationByEmployeeId(String employeeId);
    AddressVerificationDTO updateAddressVerification(String employeeId, AddressVerificationUpdateRequest request);
    AddressVerificationDTO verifyHomeAddress(String employeeId, String verifiedBy);
    AddressVerificationDTO verifyWorkAddress(String employeeId, String verifiedBy);
    List<AddressVerificationDTO> getAllAddressVerifications();
}

