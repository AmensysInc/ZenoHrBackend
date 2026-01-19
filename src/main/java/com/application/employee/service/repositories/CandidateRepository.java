package com.application.employee.service.repositories;

import com.application.employee.service.entities.Candidate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CandidateRepository extends JpaRepository<Candidate,String> {
    Optional<Candidate> findByEmailAddress(String emailId);
    List<Candidate> findByCandidateStatus(String status);
    Page<Candidate> findAll(Specification<Candidate> spec, Pageable pageable);
    List<Candidate> findAll(Specification<Candidate> spec);

//    @Query("SELECT c FROM Candidate c WHERE :spec MEMBER OF c.specifications")
//    List<Candidate> findBySpecification(@Param("spec") Specification<Candidate> spec);
}
