package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.Message;
import com.application.employee.service.repositories.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MessageServiceImpl {

    @Autowired
    private MessageRepository repository;

    public Message save(Message message) {
        return repository.save(message);
    }

    public List<Message> getAll() {
        return repository.findAll();
    }

    public Optional<Message> getById(Long id) {
        return repository.findById(id);
    }

    public Message update(Long id, Message newData) {
        return repository.findById(id).map(msg -> {
            msg.setName(newData.getName());
            msg.setSubject(newData.getSubject());
            msg.setBody(newData.getBody());
            msg.setDescription(newData.getDescription());
            msg.setCategory(newData.getCategory());
            msg.setIsActive(newData.getIsActive());
            return repository.save(msg);
        }).orElseThrow(() -> new RuntimeException("Message not found"));
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}

