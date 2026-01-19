package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.entities.PurchaseOrder;
import com.application.employee.service.exceptions.ResourceNotFoundException;
import com.application.employee.service.repositories.PurchaseOrderRepository;
import com.application.employee.service.services.EmployeeService;
import com.application.employee.service.services.PurchaseOrderService;
import com.application.employee.service.specifications.EmployeeSpecifications;
import com.application.employee.service.specifications.PurchaseOrderSpecifications;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Service
public class PurchaseOrderImplementation implements PurchaseOrderService {
    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;
    @Autowired
    private EmployeeService employeeService;


        @Override
    public PurchaseOrder saveOrder(PurchaseOrder order) {
        String randomOrderID = UUID.randomUUID().toString();
        order.setOrderId(randomOrderID);
            Employee employee = employeeService.getEmployee(order.getEmployee().getEmployeeID());
            order.setEmployeeFirstName(employee.getFirstName());
            order.setEmployeeLastName(employee.getLastName());
        return purchaseOrderRepository.save(order);
    }

    @Override
    public List<PurchaseOrder> getAllOrders() {

        return purchaseOrderRepository.findAll();
    }

    @Override
    public PurchaseOrder getOrder(String id) {
        return purchaseOrderRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Order not found with given orderID: " + id));
    }

    @Override
    public PurchaseOrder updateOrder(String id, PurchaseOrder updatedOrder) {
        PurchaseOrder existingOrder = getOrder(id);

        existingOrder.setDateOfJoining(updatedOrder.getDateOfJoining());
        existingOrder.setProjectEndDate(updatedOrder.getProjectEndDate());
        existingOrder.setBillRate(updatedOrder.getBillRate());
        existingOrder.setEndClientName(updatedOrder.getEndClientName());
        existingOrder.setVendorPhoneNo(updatedOrder.getVendorPhoneNo());
        existingOrder.setVendorEmailId(updatedOrder.getVendorEmailId());

        return purchaseOrderRepository.save(existingOrder);
    }

    @Override
    public void deleteOrder(String id) {
        PurchaseOrder order = getOrder(id);
        purchaseOrderRepository.delete(order);
    }

    @Override
    public Page<PurchaseOrder> findOrderWithPagination(int page, int size, String searchField, String searchString) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<PurchaseOrder> spec = Specification.where(null);

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "dateOfJoining":
                    spec = spec.and(PurchaseOrderSpecifications.dateOfJoiningEquals(searchString));
                    break;
                case "projectEndDate":
                    spec = spec.and(PurchaseOrderSpecifications.projectEndDateEquals(searchString));
                    break;
                case "billRate":
                    Integer billRateValue = Integer.parseInt(searchString);
                    spec = spec.and(PurchaseOrderSpecifications.billRateEquals(billRateValue));
                    break;
                case "endClientName":
                    spec = spec.and(PurchaseOrderSpecifications.endClientNameEquals(searchString));
                    break;
                case "vendorPhoneNo":
                    spec = spec.and(PurchaseOrderSpecifications.vendorPhoneNoEquals(searchString));
                    break;
                case "vendorEmailId":
                    spec = spec.and(PurchaseOrderSpecifications.vendorEmailIdEquals(searchString));
                    break;
            }
        }
        return purchaseOrderRepository.findAll(spec, pageable);
    }

    @Override
    public Page<PurchaseOrder> findOrderWithEmployeeID(int page, int size, String searchField, String searchString, String employeeID) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<PurchaseOrder> spec = Specification.where(null);

        if (!employeeID.isEmpty()){
            spec = spec.and(PurchaseOrderSpecifications.employeeIDEquals(employeeID));
        }

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "dateOfJoining":
                    spec = spec.and(PurchaseOrderSpecifications.dateOfJoiningEquals(searchString));
                    break;
                case "projectEndDate":
                    spec = spec.and(PurchaseOrderSpecifications.projectEndDateEquals(searchString));
                    break;
                case "billRate":
                    Integer billRateValue = Integer.parseInt(searchString);
                    spec = spec.and(PurchaseOrderSpecifications.billRateEquals(billRateValue));
                    break;
                case "endClientName":
                    spec = spec.and(PurchaseOrderSpecifications.endClientNameEquals(searchString));
                    break;
                case "vendorPhoneNo":
                    spec = spec.and(PurchaseOrderSpecifications.vendorPhoneNoEquals(searchString));
                    break;
                case "vendorEmailId":
                    spec = spec.and(PurchaseOrderSpecifications.vendorEmailIdEquals(searchString));
                    break;
            }
        }
        return purchaseOrderRepository.findAll(spec, pageable);
    }

    @Override
    public Page<PurchaseOrder> findOrdersByCompanyId(int page, int size, Long companyId) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dateOfJoining"));
        return purchaseOrderRepository.findByCompanyIdOrAll(companyId, pageable);
    }

}
