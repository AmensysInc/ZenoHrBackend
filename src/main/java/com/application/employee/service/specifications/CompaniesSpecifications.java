package com.application.employee.service.specifications;

import com.application.employee.service.entities.Companies;
import org.springframework.data.jpa.domain.Specification;

public class CompaniesSpecifications {
    public static Specification<Companies> companyNameEquals(String companyName) {
        return (root, query, criteriaBuilder) -> {
            if (companyName != null) {
                String searchPattern = "%" + companyName.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("companyName")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Companies> emailEquals(String email) {
        return (root, query, criteriaBuilder) -> {
            if (email != null) {
                String searchPattern = "%" + email.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), searchPattern);
            }
            return null;
        };
    }
}
