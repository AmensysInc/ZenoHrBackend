package com.application.employee.service.specifications;

import com.application.employee.service.entities.VisaDetails;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

public class VisaDetailsSpecifications {
    public static Specification<VisaDetails> visaTypeEquals(String visaType) {
        return (root, query, criteriaBuilder) -> {
            if (visaType != null) {
                String searchPattern = "%" + visaType.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("visaType")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<VisaDetails> visaStartDateEquals(String visaStartDate) {
        return (root, query, criteriaBuilder) -> {
            if (visaStartDate != null && !visaStartDate.isEmpty()) {
                String likeExpression = "%" + visaStartDate + "%";
                return criteriaBuilder.like(root.get("visaStartDate").as(String.class), likeExpression);
            }
            return null;
        };
    }

public static Specification<VisaDetails> visaExpiryDateEquals(String visaExpiryDate) {
    return (root, query, criteriaBuilder) -> {
        if (visaExpiryDate != null && !visaExpiryDate.isEmpty()) {
            String likeExpression = "%" + visaExpiryDate + "%";
            return criteriaBuilder.like(root.get("visaExpiryDate").as(String.class), likeExpression);
        }
        return null;
    };
}


    public static Specification<VisaDetails> employeeIDEquals(String employeeID) {
        return (root, query, criteriaBuilder) -> {
            if (employeeID != null) {
                return criteriaBuilder.equal(root.get("employee").get("employeeID"), employeeID);
            }
            return null;
        };
    }
}
