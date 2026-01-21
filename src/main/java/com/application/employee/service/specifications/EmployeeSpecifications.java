package com.application.employee.service.specifications;

import com.application.employee.service.entities.Employee;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;

public class EmployeeSpecifications {

    public static Specification<Employee> firstNameContains(String partialFirstName) {
        return (root, query, criteriaBuilder) -> {
            if (partialFirstName != null) {
                String searchPattern = "%" + partialFirstName.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchPattern);
            }
            return null;
        };
    }

    public static Specification<Employee> emailIDEquals(String emailID) {
        return (root, query, criteriaBuilder) -> {
            if (emailID != null) {
                String searchPattern = "%" + emailID.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("emailID")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Employee> phoneNoEquals(String phoneNo) {
        return (root, query, criteriaBuilder) -> {
            if (phoneNo != null) {
                String searchPattern = "%" + phoneNo.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNo")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Employee> middleNameEquals(String middleName) {
        return (root, query, criteriaBuilder) -> {
            if (middleName != null) {
                String searchPattern = "%" + middleName.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("middleName")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Employee> lastNameEquals(String lastName) {
        return (root, query, criteriaBuilder) -> {
            if (lastName != null) {
                String searchPattern = "%" + lastName.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Employee> clgOfGradEquals(String clgOfGrad) {
        return (root, query, criteriaBuilder) -> {
            if (clgOfGrad != null) {
                String searchPattern = "%" + clgOfGrad.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("clgOfGrad")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Employee> dobEquals(LocalDate dob) {
        return (root, query, criteriaBuilder) -> {
            if (dob != null) {
                String searchPattern = "%" + dob.toString() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("dob")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Employee> onBenchEquals(String onBench) {
        return (root, query, criteriaBuilder) -> {
            if (onBench != null) {
                String searchPattern = "%" + onBench.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("onBench")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Employee> companyContains(String company) {
        return (root, query, criteriaBuilder) -> {
            if (company != null) {
                String searchPattern = "%" + company.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("company")), searchPattern);
            }
            return null;
        };
    }

    public static Specification<Employee> companyIdEquals(Long companyId) {
        return (root, query, criteriaBuilder) -> {
            if (companyId != null) {
                return criteriaBuilder.equal(root.get("company").get("companyId"), companyId.intValue());
            }
            return null;
        };
    }

}
