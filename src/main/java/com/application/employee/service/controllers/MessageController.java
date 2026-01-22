package com.application.employee.service.controllers;

import com.application.employee.service.entities.Message;
import com.application.employee.service.services.implementations.MessageServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
public class MessageController {

    @Autowired
    private MessageServiceImpl service;

    @PostMapping
    public ResponseEntity<Message> create(@RequestBody Message message) {
        return ResponseEntity.ok(service.save(message));
    }

    @GetMapping
    public ResponseEntity<List<Message>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Message> getById(@PathVariable Long id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Message> update(@PathVariable Long id, @RequestBody Message message) {
        return ResponseEntity.ok(service.update(id, message));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/initialize-defaults")
    public ResponseEntity<String> initializeDefaultTemplates() {
        service.initializeDefaultTemplates();
        return ResponseEntity.ok("Default email templates initialized successfully");
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<Message> getByCategory(@PathVariable String category) {
        return service.getByCategory(category)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
