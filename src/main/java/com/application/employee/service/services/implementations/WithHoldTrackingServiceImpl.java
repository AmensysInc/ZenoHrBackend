package com.application.employee.service.services.implementations;
import com.application.employee.service.dto.EmployeeWithHoldDTO;
import com.application.employee.service.entities.CompanyBalanceProjection;
import com.application.employee.service.entities.WithHoldTracking;
import com.application.employee.service.repositories.WithHoldTrackingRepository;
import com.application.employee.service.services.WithHoldTrackingService;
import com.application.employee.service.specifications.WithHoldTrackingSpecifications;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class WithHoldTrackingServiceImpl implements WithHoldTrackingService {
    @Autowired
    private WithHoldTrackingRepository repository;

    @Override
    public List<WithHoldTracking> getAllWithHoldTracking() {
        return repository.findAll();
    }

    @Override
    public WithHoldTracking getWithHoldTrackingById(String id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public WithHoldTracking saveWithHoldTracking(WithHoldTracking withHoldTracking) {
        String randomTrackingId = UUID.randomUUID().toString();
        withHoldTracking.setTrackingId(randomTrackingId);

        BigDecimal actualHours = withHoldTracking.getActualHours();
        BigDecimal actualRate = withHoldTracking.getActualRate();
        BigDecimal paidHours = withHoldTracking.getPaidHours();
        BigDecimal paidRate = withHoldTracking.getPaidRate();
        withHoldTracking.setType(withHoldTracking.getType());
        withHoldTracking.setStatus(withHoldTracking.getStatus());
        withHoldTracking.setBillrate(withHoldTracking.getBillrate());

        if (actualHours != null && actualRate != null) {
            BigDecimal actualAmount = actualHours.multiply(actualRate);
            withHoldTracking.setActualAmt(actualAmount);
            if (!withHoldTracking.getActualAmt().equals(actualAmount)) {
                throw new IllegalArgumentException("Actual amount is not consistent.");
            }
        }

        if (paidHours != null && paidRate != null) {
            BigDecimal paidAmount = paidHours.multiply(paidRate);
            withHoldTracking.setPaidAmt(paidAmount);
            if (!withHoldTracking.getPaidAmt().equals(paidAmount)) {
                throw new IllegalArgumentException("Paid amount is not consistent.");
            }
        }

        if (withHoldTracking.getActualAmt() != null && withHoldTracking.getPaidAmt() != null) {
            BigDecimal balance = withHoldTracking.getActualAmt().subtract(withHoldTracking.getPaidAmt());
            withHoldTracking.setBalance(balance);
            if (!withHoldTracking.getBalance().equals(balance)) {
                throw new IllegalArgumentException("Balance amount is not consistent.");
            }
        }

        return repository.save(withHoldTracking);
    }


    @Override
    public WithHoldTracking updateWithHoldTracking(String trackingId, WithHoldTracking updatedTracking) {
        WithHoldTracking existingTracking = getWithHoldTrackingById(trackingId);

        existingTracking.setMonth(updatedTracking.getMonth());
        existingTracking.setYear(updatedTracking.getYear());
        existingTracking.setProjectName(updatedTracking.getProjectName());
        existingTracking.setActualHours(updatedTracking.getActualHours());
        existingTracking.setActualRate(updatedTracking.getActualRate());
        existingTracking.setActualAmt(updatedTracking.getActualAmt());
        existingTracking.setPaidHours(updatedTracking.getPaidHours());
        existingTracking.setPaidRate(updatedTracking.getPaidRate());
        existingTracking.setPaidAmt(updatedTracking.getPaidAmt());
        existingTracking.setBalance(updatedTracking.getBalance());
        existingTracking.setExcelData(updatedTracking.getExcelData());
        existingTracking.setType(updatedTracking.getType());
        existingTracking.setStatus(updatedTracking.getStatus());
        existingTracking.setBillrate(updatedTracking.getBillrate());

        return repository.save(existingTracking);
    }

    @Override
    public void deleteTracking(String id) {
        WithHoldTracking tracking = getWithHoldTrackingById(id);
        repository.delete(tracking);
    }

    @Override
    public Page<WithHoldTracking> findTrackingWithPagination(int page, int size, String searchField, String searchString) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<WithHoldTracking> spec = Specification.where(null);


        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "month":
                    spec = spec.and(WithHoldTrackingSpecifications.monthEquals(searchString));
                    break;
                case "year":
                    spec = spec.and(WithHoldTrackingSpecifications.yearEquals(searchString));
                    break;
                case "projectName":
                    spec = spec.and(WithHoldTrackingSpecifications.projectNameEquals(searchString));
                    break;
                case "actualHours":
                    BigDecimal actualHours = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.actualHoursEquals(actualHours));
                    break;
                case "actualRate":
                    BigDecimal actualRate = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.actualRateEquals(actualRate));
                    break;
                case "actualAmt":
                    BigDecimal actualAmt = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.actualAmtEquals(actualAmt));
                    break;
                case "paidHours":
                    BigDecimal paidHours = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.paidHoursEquals(paidHours));
                    break;
                case "paidRate":
                    BigDecimal paidRate = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.paidRateEquals(paidRate));
                    break;
                case "paidAmt":
                    BigDecimal paidAmt = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.paidAmtEquals(paidAmt));
                    break;
                case "balance":
                    BigDecimal balance = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.balanceEquals(balance));
                    break;
            }
        }

        return repository.findAll(spec, pageable);
    }

    @Override
    public Page<WithHoldTracking> findTrackingWithEmployeeID(int page, int size, String searchField, String searchString, String employeeID) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<WithHoldTracking> spec = Specification.where(null);

        if (!employeeID.isEmpty()){
            spec = spec.and(WithHoldTrackingSpecifications.employeeIDEquals(employeeID));
        }

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "month":
                    spec = spec.and(WithHoldTrackingSpecifications.monthEquals(searchString));
                    break;
                case "year":
                    spec = spec.and(WithHoldTrackingSpecifications.yearEquals(searchString));
                    break;
                case "projectName":
                    spec = spec.and(WithHoldTrackingSpecifications.projectNameEquals(searchString));
                    break;
                case "actualHours":
                    BigDecimal actualHours = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.actualHoursEquals(actualHours));
                    break;
                case "actualRate":
                    BigDecimal actualRate = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.actualRateEquals(actualRate));
                    break;
                case "actualAmt":
                    BigDecimal actualAmt = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.actualAmtEquals(actualAmt));
                    break;
                case "paidHours":
                    BigDecimal paidHours = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.paidHoursEquals(paidHours));
                    break;
                case "paidRate":
                    BigDecimal paidRate = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.paidRateEquals(paidRate));
                    break;
                case "paidAmt":
                    BigDecimal paidAmt = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.paidAmtEquals(paidAmt));
                    break;
                case "balance":
                    BigDecimal balance = new BigDecimal(searchString);
                    spec = spec.and(WithHoldTrackingSpecifications.balanceEquals(balance));
                    break;
            }
        }

        return repository.findAll(spec, pageable);
    }

    @Override
    public List<CompanyBalanceProjection> getCompanyWiseBalance() {
        return repository.findCompanyWiseBalance();
    }

    @Override
    public List<EmployeeWithHoldDTO> getWithHoldByCompany(Integer companyId) {
        return repository.findEmployeeWithHoldByCompany(companyId);
    }
}
