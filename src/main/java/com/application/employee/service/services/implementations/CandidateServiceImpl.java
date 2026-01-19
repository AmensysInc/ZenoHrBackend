package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Candidate;
import com.application.employee.service.exceptions.ResourceNotFoundException;
import com.application.employee.service.repositories.CandidateRepository;
import com.application.employee.service.services.CandidateService;
import com.application.employee.service.specifications.CandidateSpecification;
import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class CandidateServiceImpl implements CandidateService {
    @Autowired
    private CandidateRepository candidateRepository;
    @Override
    public Candidate saveCandidate(Candidate candidate) {
        Optional<Candidate> existingCandidate = candidateRepository.findByEmailAddress(candidate.getEmailAddress());
        if (existingCandidate.isPresent()){
            return null;
        }
        Ulid ulid = UlidCreator.getUlid();
        String candidateId = ulid.toString();
        candidate.setCandidateID(candidateId);

        Candidate savedCandidate = candidateRepository.save(candidate);
        return savedCandidate;
    }

    @Override
    public List<Candidate> getAllCandidates() {
        return candidateRepository.findAll();
    }

    @Override
    public Candidate getCandidate(String id) {
        return candidateRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Candidate with given ID not found : " + id )
        );
    }

    @Override
    public Candidate updateCandidate(String id, Candidate candidate) {
        candidate.setCandidateID(id);
        return candidateRepository.save(candidate);
    }
    @Override
    public void deleteCandidate(String id) {
        Candidate candidate = candidateRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Candidate with given ID not found : " + id )
        );
        candidateRepository.delete(candidate);
    }

    @Override
    public List<Candidate> getCandidatesByStatus(String status) {
        return candidateRepository.findByCandidateStatus(status);
    }

    @Override
    public Page<Candidate> findCandidateWithPagination(int page, int size, String searchField, String searchString) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<Candidate> spec = Specification.where(null);

        spec = spec.and(CandidateSpecification.candidateStatusNotEquals("InMarketing"));

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "firstName":
                    spec = spec.and(CandidateSpecification.firstNameEquals(searchString));
                    break;
                case "lastName":
                    spec = spec.and(CandidateSpecification.lastNameEquals(searchString));
                    break;
                case "recruiterName":
                    spec = spec.and(CandidateSpecification.recruiterNameEquals(searchString));
                    break;
                case "company":
                    spec = spec.and(CandidateSpecification.companyEquals(searchString));
                    break;
                case "emailAddress":
                    spec = spec.and(CandidateSpecification.emailAddressEquals(searchString));
                    break;
                case "skills":
                    spec = spec.and(CandidateSpecification.skillsEquals(searchString));
                    break;
                case "phoneNo":
                    spec = spec.and(CandidateSpecification.phoneNoEquals(searchString));
                    break;
                case "originalVisaStatus":
                    spec = spec.and(CandidateSpecification.originalVisaStatusEquals(searchString));
                    break;
                case "comments":
                    spec = spec.and(CandidateSpecification.commentsEquals(searchString));
                    break;
                case "candidateStatus":
                    spec = spec.and(CandidateSpecification.candidateStatusEquals(searchString));
                    break;
            }
        }

        return candidateRepository.findAll(spec, pageable);
    }

    @Override
    public List<Candidate> findCandidateWithoutPagination(String searchField, String searchString) {
        Specification<Candidate> spec = Specification.where(null);

        spec = spec.and(CandidateSpecification.candidateStatusNotEquals("InMarketing"));

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "firstName":
                    spec = spec.and(CandidateSpecification.firstNameEquals(searchString));
                    break;
                case "lastName":
                    spec = spec.and(CandidateSpecification.lastNameEquals(searchString));
                    break;
                case "recruiterName":
                    spec = spec.and(CandidateSpecification.recruiterNameEquals(searchString));
                    break;
                case "emailAddress":
                    spec = spec.and(CandidateSpecification.emailAddressEquals(searchString));
                    break;
                case "skills":
                    spec = spec.and(CandidateSpecification.skillsEquals(searchString));
                    break;
                case "phoneNo":
                    spec = spec.and(CandidateSpecification.phoneNoEquals(searchString));
                    break;
                case "originalVisaStatus":
                    spec = spec.and(CandidateSpecification.originalVisaStatusEquals(searchString));
                    break;
                case "comments":
                    spec = spec.and(CandidateSpecification.commentsEquals(searchString));
                    break;
                case "candidateStatus":
                    spec = spec.and(CandidateSpecification.candidateStatusEquals(searchString));
                    break;
            }
        }

        return candidateRepository.findAll(spec);
    }

    @Override
    public Page<Candidate> findCandidateWithPaginationInMarketing(int page, int size, String searchField, String searchString) {
        Pageable pageable = PageRequest.of(page, size);

        Specification<Candidate> spec = Specification.where(CandidateSpecification.candidateStatusEquals("InMarketing"));

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "firstName":
                    spec = spec.and(CandidateSpecification.firstNameEquals(searchString));
                    break;
                case "lastName":
                    spec = spec.and(CandidateSpecification.lastNameEquals(searchString));
                    break;
                case "recruiterName":
                    spec = spec.and(CandidateSpecification.recruiterNameEquals(searchString));
                    break;
                case "emailAddress":
                    spec = spec.and(CandidateSpecification.emailAddressEquals(searchString));
                    break;
                case "skills":
                    spec = spec.and(CandidateSpecification.skillsEquals(searchString));
                    break;
                case "phoneNo":
                    spec = spec.and(CandidateSpecification.phoneNoEquals(searchString));
                    break;
                case "originalVisaStatus":
                    spec = spec.and(CandidateSpecification.originalVisaStatusEquals(searchString));
                    break;
                case "comments":
                    spec = spec.and(CandidateSpecification.commentsEquals(searchString));
                    break;
                case "candidateStatus":
                    spec = spec.and(CandidateSpecification.candidateStatusEquals(searchString));
                    break;
            }
        }

        return candidateRepository.findAll(spec, pageable);
    }

    @Override
    public List<Candidate> findCandidateWithoutPaginationInMarketing(String searchField, String searchString) {
        Specification<Candidate> spec = Specification.where(CandidateSpecification.candidateStatusEquals("InMarketing"));

        if (searchField != null && !searchField.isEmpty() && searchString != null && !searchString.isEmpty()) {
            switch (searchField) {
                case "firstName":
                    spec = spec.and(CandidateSpecification.firstNameEquals(searchString));
                    break;
                case "lastName":
                    spec = spec.and(CandidateSpecification.lastNameEquals(searchString));
                    break;
                case "recruiterName":
                    spec = spec.and(CandidateSpecification.recruiterNameEquals(searchString));
                    break;
                case "emailAddress":
                    spec = spec.and(CandidateSpecification.emailAddressEquals(searchString));
                    break;
                case "skills":
                    spec = spec.and(CandidateSpecification.skillsEquals(searchString));
                    break;
                case "phoneNo":
                    spec = spec.and(CandidateSpecification.phoneNoEquals(searchString));
                    break;
                case "originalVisaStatus":
                    spec = spec.and(CandidateSpecification.originalVisaStatusEquals(searchString));
                    break;
                case "comments":
                    spec = spec.and(CandidateSpecification.commentsEquals(searchString));
                    break;
                case "candidateStatus":
                    spec = spec.and(CandidateSpecification.candidateStatusEquals(searchString));
                    break;
            }
        }

        return candidateRepository.findAll(spec);
    }

}
