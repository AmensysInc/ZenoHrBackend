package com.application.employee.service.specifications;

import com.application.employee.service.entities.PurchaseOrder;
import org.springframework.data.jpa.domain.Specification;

public class PurchaseOrderSpecifications {
    public static Specification<PurchaseOrder> dateOfJoiningEquals(String dateOfJoining) {
        return (root, query, criteriaBuilder) -> {
            if (dateOfJoining != null && !dateOfJoining.isEmpty()) {
                String likeExpression = "%" + dateOfJoining + "%";
                return criteriaBuilder.like(root.get("dateOfJoining").as(String.class), likeExpression);
            }
            return null;
        };
    }
    public static Specification<PurchaseOrder> projectEndDateEquals(String projectEndDate) {
        return (root, query, criteriaBuilder) -> {
            if (projectEndDate != null && !projectEndDate.isEmpty()) {
                String likeExpression = "%" + projectEndDate + "%";
                return criteriaBuilder.like(root.get("projectEndDate").as(String.class), likeExpression);
            }
            return null;
        };
    }
    public static Specification<PurchaseOrder> billRateEquals(Integer billRate) {
        return (root, query, criteriaBuilder) -> {
            if (billRate != null) {
                String searchPattern = "%" + billRate + "%";
                return criteriaBuilder.like(root.get("billRate").as(String.class), searchPattern);
            }
            return null;
        };
    }


    public static Specification<PurchaseOrder> endClientNameEquals(String endClientName) {
        return (root, query, criteriaBuilder) -> {
            if (endClientName != null) {
                String searchPattern = "%" + endClientName.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("endClientName")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<PurchaseOrder> vendorPhoneNoEquals(String vendorPhoneNo) {
        return (root, query, criteriaBuilder) -> {
            if (vendorPhoneNo != null) {
                String searchPattern = "%" + vendorPhoneNo.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("vendorPhoneNo")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<PurchaseOrder> vendorEmailIdEquals(String vendorEmailId) {
        return (root, query, criteriaBuilder) -> {
            if (vendorEmailId != null) {
                String searchPattern = "%" + vendorEmailId.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("vendorEmailId")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<PurchaseOrder> employeeIDEquals(String employeeID) {
        return (root, query, criteriaBuilder) -> {
            if (employeeID != null) {
                return criteriaBuilder.equal(root.get("employee").get("employeeID"), employeeID);
            }
            return null;
        };
    }
}
