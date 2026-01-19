package com.application.employee.service.controllers;

import com.application.employee.service.entities.Candidate;
import com.application.employee.service.services.CandidateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/candidates")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class CandidateController {
    @Autowired
    private CandidateService candidateService;
    @PostMapping
    public ResponseEntity<String> createCandidate(@RequestBody Candidate candidate){
        Candidate candidates = candidateService.saveCandidate(candidate);
        if (candidates == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Candidate already exists for given EmailID");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("Candidate created successfully");
    }
    @PutMapping("/{candidateId}")
    public ResponseEntity<String> editCandidate(@PathVariable String candidateId, @RequestBody Candidate candidate){
        Candidate updateCandidate = candidateService.updateCandidate(candidateId,candidate);
        return ResponseEntity.ok("Candidate updated successfully");
    }
    @GetMapping
    public ResponseEntity<?> getAllCandidates(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(name = "searchField",defaultValue = "") String field,
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString
    ){
        if(page == null && size == null){
            List<Candidate> candidates = candidateService.findCandidateWithoutPagination(field, seacrhString);
            return ResponseEntity.ok(candidates);
        }
        Page<Candidate> candidates = candidateService.findCandidateWithPagination(page, size, field, seacrhString);
        return ResponseEntity.ok(candidates);
    }
    @GetMapping("/{candidateId}")
    public ResponseEntity<Candidate> getCandidateById(@PathVariable String candidateId){
        Candidate candidate = candidateService.getCandidate(candidateId);
        return ResponseEntity.ok(candidate);
    }
    @GetMapping("/inMarketing")
    public ResponseEntity<?> getCandidatesInMarketing(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(name = "searchField",defaultValue = "") String field,
            @RequestParam(name = "searchString",defaultValue = "") String seacrhString
    ) {
        if(page == null && size == null){
            List<Candidate> candidatesInMarketing = candidateService.findCandidateWithoutPaginationInMarketing(field, seacrhString);
            return ResponseEntity.ok(candidatesInMarketing);
        }
        Page<Candidate> candidatesInMarketing = candidateService.findCandidateWithPaginationInMarketing(page, size, field, seacrhString);
        return ResponseEntity.ok(candidatesInMarketing);
    }
    @DeleteMapping("/{candidateId}")
    public ResponseEntity<Void> deleteCandidate(@PathVariable String candidateId){
        candidateService.deleteCandidate(candidateId);
        return ResponseEntity.noContent().build();
    }
}
