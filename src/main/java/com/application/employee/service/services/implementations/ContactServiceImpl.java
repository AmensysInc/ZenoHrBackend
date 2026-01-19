package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Contacts;
import com.application.employee.service.repositories.ContactsRepository;
import com.application.employee.service.services.ContactService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ContactServiceImpl implements ContactService {
    @Autowired
    private ContactsRepository contactsRepository;
    @Override
    public List<Contacts> getAllContacts() {
        return contactsRepository.findAll();
    }

    @Override
    public Contacts getContactsById(Long id) {
        return contactsRepository.findById(id).orElse(null);
    }

    @Override
    public Contacts createContacts(Contacts contacts) {
        return contactsRepository.save(contacts);
    }

    @Override
    public Contacts updateContacts(Long id, Contacts contacts) {
        if (contactsRepository.existsById(id)) {
            contacts.setId(id);
            return contactsRepository.save(contacts);
        } else {
            return null;
        }
    }

    @Override
    public List<Contacts> getContactsByRecruiterId(String recruiterId) {
        return contactsRepository.findByRecruiterId(recruiterId);
    }

    @Override
    public void deleteContacts(Long id) {
        contactsRepository.deleteById(id);
    }

    @Override
    public List<String> getAllEmails() {
        return contactsRepository.findEmails();
    }
}
