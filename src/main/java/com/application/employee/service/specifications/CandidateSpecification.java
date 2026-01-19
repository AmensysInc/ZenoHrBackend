package com.application.employee.service.specifications;

import com.application.employee.service.entities.Candidate;
import org.springframework.data.jpa.domain.Specification;

public class CandidateSpecification {
    public static Specification<Candidate> firstNameEquals(String firstName) {
        return (root, query, criteriaBuilder) -> {
            if (firstName != null) {
                String searchPattern = "%" + firstName.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Candidate> lastNameEquals(String lastName) {
        return (root, query, criteriaBuilder) -> {
            if (lastName != null) {
                String searchPattern = "%" + lastName.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Candidate> emailAddressEquals(String emailAddress) {
        return (root, query, criteriaBuilder) -> {
            if (emailAddress != null) {
                String searchPattern = "%" + emailAddress.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("emailAddress")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Candidate> phoneNoEquals(String phoneNo) {
        return (root, query, criteriaBuilder) -> {
            if (phoneNo != null) {
                String searchPattern = "%" + phoneNo.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNo")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Candidate> recruiterNameEquals(String recruiterName) {
        return (root, query, criteriaBuilder) -> {
            if (recruiterName != null) {
                String searchPattern = "%" + recruiterName.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("recruiterName")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Candidate> skillsEquals(String skills) {
        return (root, query, criteriaBuilder) -> {
            if (skills != null) {
                String searchPattern = "%" + skills.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("skills")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Candidate> originalVisaStatusEquals(String originalVisaStatus) {
        return (root, query, criteriaBuilder) -> {
            if (originalVisaStatus != null) {
                String searchPattern = "%" + originalVisaStatus.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("originalVisaStatus")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Candidate> commentsEquals(String comments) {
        return (root, query, criteriaBuilder) -> {
            if (comments != null) {
                String searchPattern = "%" + comments.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("comments")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Candidate> companyEquals(String company) {
        return (root, query, criteriaBuilder) -> {
            if (company != null) {
                String searchPattern = "%" + company.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("company")), searchPattern);
            }
            return null;
        };
    }
    public static Specification<Candidate> candidateStatusEquals(String candidateStatus) {
        return (root, query, criteriaBuilder) -> {
            if (candidateStatus != null) {
                String searchPattern = "%" + candidateStatus.toLowerCase() + "%";
                return criteriaBuilder.like(criteriaBuilder.lower(root.get("candidateStatus")), searchPattern);
            }
            return null;
        };
    }

    public static Specification<Candidate> candidateStatusNotEquals(String candidateStatus) {
        return (root, query, criteriaBuilder) -> {
            if (candidateStatus != null) {
                return criteriaBuilder.notEqual(root.get("candidateStatus"), candidateStatus);
            }
            return null;
        };
    }

}
