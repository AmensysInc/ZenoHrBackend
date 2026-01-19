package com.application.employee.service.services;

import com.application.employee.service.entities.Candidate;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CandidateService {
    Candidate saveCandidate(Candidate candidate);
    List<Candidate> getAllCandidates();
    Candidate getCandidate(String id);
    Candidate updateCandidate(String id, Candidate candidate);
    void deleteCandidate(String id);
    List<Candidate> getCandidatesByStatus(String status);
    Page<Candidate> findCandidateWithPagination(int page, int size, String field, String seacrhString);

    List<Candidate> findCandidateWithoutPagination(String searchField, String searchString);

    Page<Candidate> findCandidateWithPaginationInMarketing(int page, int size, String searchField, String searchString);

    List<Candidate> findCandidateWithoutPaginationInMarketing(String searchField, String searchString);
}
