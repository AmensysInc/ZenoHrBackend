package com.application.employee.service.controllers;

import com.application.employee.service.entities.Contacts;
import com.application.employee.service.services.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/contacts")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ContactController {
    @Autowired
    private ContactService contactService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') || hasRole('RECRUITER') || hasRole('SADMIN')")
    public List<Contacts> getAllContacts() {
        return contactService.getAllContacts();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') || hasRole('RECRUITER') || hasRole('SADMIN')")
    public ResponseEntity<Contacts> getContactsById(@PathVariable Long id) {
        Contacts contacts = contactService.getContactsById(id);
        if (contacts != null) {
            return new ResponseEntity<>(contacts, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') || hasRole('RECRUITER') || hasRole('SADMIN')")
    public ResponseEntity<String> createContact(@RequestBody Contacts contacts) {
        Contacts createdContacts = contactService.createContacts(contacts);
        if (createdContacts == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Contact already exists for given EmailID");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body("Contact created successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') || hasRole('RECRUITER') || hasRole('SADMIN')")
    public ResponseEntity<Contacts> updateEmployee(@PathVariable Long id, @RequestBody Contacts contacts) {
        Contacts updatedContacts = contactService.updateContacts(id, contacts);
        if (updatedContacts != null) {
            return new ResponseEntity<>(updatedContacts, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/recruiter/{recruiterId}")
    public ResponseEntity<List<Contacts>> getContactsByRecruiterId(@PathVariable String recruiterId) {
        List<Contacts> contacts = contactService.getContactsByRecruiterId(recruiterId);
        return new ResponseEntity<>(contacts, HttpStatus.OK);
    }

    @GetMapping("/emails")
    @PreAuthorize("hasRole('ADMIN') || hasRole('RECRUITER') || hasRole('SADMIN')")
    public ResponseEntity<List<String>> getAllEmails() {
        List<String> emailIds = contactService.getAllEmails();
        return new ResponseEntity<>(emailIds, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') || hasRole('RECRUITER') || hasRole('SADMIN')")
    public ResponseEntity<Void> deleteContacts(@PathVariable Long id) {
        contactService.deleteContacts(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
