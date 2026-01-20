package com.application.employee.service.controllers;

import com.application.employee.service.entities.UserCompanyRole;
import com.application.employee.service.services.UserCompanyRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/user-company")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class UserCompanyRoleController {

    @Autowired
    private UserCompanyRoleService service;

    @PostMapping
    public ResponseEntity<UserCompanyRole> createRole(@RequestBody UserCompanyRole role) {
        return ResponseEntity.ok(service.saveUserCompanyRole(role));
    }

    @GetMapping
    public ResponseEntity<List<UserCompanyRole>> getAllRoles() {
        return ResponseEntity.ok(service.getAllRoles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserCompanyRole> getRoleById(@PathVariable Long id) {
        Optional<UserCompanyRole> role = service.getRoleById(id);
        return role.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserCompanyRole>> getRolesByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(service.getRolesByUserId(userId));
    }

    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<UserCompanyRole>> getRolesByCompanyId(@PathVariable Integer companyId) {
        return ResponseEntity.ok(service.getRolesByCompanyId(companyId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserCompanyRole> updateRole(@PathVariable Long id, @RequestBody UserCompanyRole role) {
        try {
            UserCompanyRole updated = service.updateUserCompanyRole(id, role);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        service.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}
