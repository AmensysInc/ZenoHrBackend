package com.application.employee.service.services;

import com.application.employee.service.entities.Contacts;
import java.util.List;

public interface ContactService {
    List<Contacts> getAllContacts();
    Contacts getContactsById(Long id);
    Contacts createContacts(Contacts employee);
    Contacts updateContacts(Long id, Contacts employee);
    List<Contacts> getContactsByRecruiterId(String recruiterId);
    void deleteContacts(Long id);
    List<String> getAllEmails();
}
