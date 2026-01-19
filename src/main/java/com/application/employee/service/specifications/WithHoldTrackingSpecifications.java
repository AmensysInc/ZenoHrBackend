package com.application.employee.service.specifications;

import com.application.employee.service.entities.WithHoldTracking;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class WithHoldTrackingSpecifications {
    public static Specification<WithHoldTracking> monthEquals(String month) {
        return (root, query, criteriaBuilder) -> {
            if (month != null) {
                String searchPattern = "%" + month.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("month")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<WithHoldTracking> yearEquals(String year) {
        return (root, query, criteriaBuilder) -> {
            if (year != null) {
                String searchPattern = "%" + year.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("year")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<WithHoldTracking> projectNameEquals(String projectName) {
        return (root, query, criteriaBuilder) -> {
            if (projectName != null) {
                String searchPattern = "%" + projectName.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("projectName")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<WithHoldTracking> actualHoursEquals(BigDecimal actualHours) {
        return (root, query, criteriaBuilder) -> {
            if (actualHours != null) {
                String searchPattern = "%" + actualHours + "%";
                return criteriaBuilder.like(root.get("actualHours").as(String.class), searchPattern);
            }
            return null;
        };
    }
    public static Specification<WithHoldTracking> actualRateEquals(BigDecimal actualRate) {
        return (root, query, criteriaBuilder) -> {
            if (actualRate != null) {
                String searchPattern = "%" + actualRate + "%";
                return criteriaBuilder.like(root.get("actualRate").as(String.class), searchPattern);
            }
            return null;
        };
    }
    public static Specification<WithHoldTracking> actualAmtEquals(BigDecimal actualAmt) {
        return (root, query, criteriaBuilder) -> {
            if (actualAmt != null) {
                String searchPattern = "%" + actualAmt + "%";
                return criteriaBuilder.like(root.get("actualAmt").as(String.class), searchPattern);
            }
            return null;
        };
    }
    public static Specification<WithHoldTracking> paidHoursEquals(BigDecimal paidHours) {
        return (root, query, criteriaBuilder) -> {
            if (paidHours != null) {
                String searchPattern = "%" + paidHours + "%";
                return criteriaBuilder.like(root.get("paidHours").as(String.class), searchPattern);
            }
            return null;
        };
    }
    public static Specification<WithHoldTracking> paidRateEquals(BigDecimal paidRate) {
        return (root, query, criteriaBuilder) -> {
            if (paidRate != null) {
                String searchPattern = "%" + paidRate + "%";
                return criteriaBuilder.like(root.get("paidRate").as(String.class), searchPattern);
            }
            return null;
        };
    }
    public static Specification<WithHoldTracking> paidAmtEquals(BigDecimal paidAmt) {
        return (root, query, criteriaBuilder) -> {
            if (paidAmt != null) {
                String searchPattern = "%" + paidAmt + "%";
                return criteriaBuilder.like(root.get("paidAmt").as(String.class), searchPattern);
            }
            return null;
        };
    }
    public static Specification<WithHoldTracking> balanceEquals(BigDecimal balance) {
        return (root, query, criteriaBuilder) -> {
            if (balance != null) {
                String searchPattern = "%" + balance + "%";
                return criteriaBuilder.like(root.get("balance").as(String.class), searchPattern);
            }
            return null;
        };
    }
    public static Specification<WithHoldTracking> employeeIDEquals(String employeeID) {
        return (root, query, criteriaBuilder) -> {
            if (employeeID != null) {
                return criteriaBuilder.equal(root.get("employee").get("employeeID"), employeeID);
            }
            return null;
        };
    }
}
