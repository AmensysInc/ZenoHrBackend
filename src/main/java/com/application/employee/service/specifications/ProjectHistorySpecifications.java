package com.application.employee.service.specifications;

import com.application.employee.service.entities.ProjectHistory;
import org.springframework.data.jpa.domain.Specification;

public class ProjectHistorySpecifications {
    public static Specification<ProjectHistory> subVendorOneEquals(String subVendorOne) {
        return (root, query, criteriaBuilder) -> {
            if (subVendorOne != null) {
                String searchPattern = "%" + subVendorOne.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("subVendorOne")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<ProjectHistory> subVendorTwoEquals(String subVendorTwo) {
        return (root, query, criteriaBuilder) -> {
            if (subVendorTwo != null) {
                String searchPattern = "%" + subVendorTwo.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("subVendorTwo")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<ProjectHistory> projectAddressEquals(String projectAddress) {
        return (root, query, criteriaBuilder) -> {
            if (projectAddress != null) {
                String searchPattern = "%" + projectAddress.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("projectAddress")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<ProjectHistory> projectStartDateEquals(String projectStartDate) {
        return (root, query, criteriaBuilder) -> {
            if (projectStartDate != null && !projectStartDate.isEmpty()) {
                String likeExpression = "%" + projectStartDate + "%";
                return criteriaBuilder.like(root.get("projectStartDate").as(String.class), likeExpression);
            }
            return null;
        };
    }
    public static Specification<ProjectHistory> projectEndDateEquals(String projectEndDate) {
        return (root, query, criteriaBuilder) -> {
            if (projectEndDate != null && !projectEndDate.isEmpty()) {
                String likeExpression = "%" + projectEndDate + "%";
                return criteriaBuilder.like(root.get("projectEndDate").as(String.class), likeExpression);
            }
            return null;
        };
    }
    public static Specification<ProjectHistory> projectStatusEquals(String projectStatus) {
        return (root, query, criteriaBuilder) -> {
            if (projectStatus != null) {
                String searchPattern = "%" + projectStatus.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("projectStatus")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<ProjectHistory> employeeIDEquals(String employeeID) {
        return (root, query, criteriaBuilder) -> {
            if (employeeID != null) {
                return criteriaBuilder.equal(root.get("employee").get("employeeID"), employeeID);
            }
            return null;
        };
    }
}
