package com.application.employee.service.services.implementations;

import com.application.employee.service.entities.UserCompanyRole;
import com.application.employee.service.repositories.UserCompanyRoleRepository;
import com.application.employee.service.services.UserCompanyRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserCompanyRoleServiceImpl implements UserCompanyRoleService {

    @Autowired
    private UserCompanyRoleRepository repository;

    @Override
    public List<UserCompanyRole> getAllRoles() {
        return repository.findAll();
    }

    @Override
    public Optional<UserCompanyRole> getRoleById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<UserCompanyRole> getRolesByUserId(String userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public List<UserCompanyRole> getRolesByCompanyId(Integer companyId) {
        return repository.findByCompanyId(companyId);
    }

    @Override
    public UserCompanyRole saveUserCompanyRole(UserCompanyRole role) {
        if ("true".equalsIgnoreCase(role.getDefaultCompany())) {
            List<UserCompanyRole> existingRoles = repository.findByUserId(role.getUserId());
            for (UserCompanyRole existing : existingRoles) {
                if (!existing.getId().equals(role.getId())) {
                    existing.setDefaultCompany("false");
                    repository.save(existing);
                }
            }
        }
        return repository.save(role);
    }

    @Override
    public UserCompanyRole updateUserCompanyRole(Long id, UserCompanyRole updatedRole) {
        Optional<UserCompanyRole> existingOpt = repository.findById(id);
        if (existingOpt.isPresent()) {
            UserCompanyRole role = existingOpt.get();
            role.setUserId(updatedRole.getUserId());
            role.setCompanyId(updatedRole.getCompanyId());
            role.setRole(updatedRole.getRole());
            role.setDefaultCompany(updatedRole.getDefaultCompany());
            role.setCreatedAt(updatedRole.getCreatedAt());

            if ("true".equalsIgnoreCase(updatedRole.getDefaultCompany())) {
                List<UserCompanyRole> existingRoles = repository.findByUserId(updatedRole.getUserId());
                for (UserCompanyRole other : existingRoles) {
                    if (!other.getId().equals(id)) {
                        other.setDefaultCompany("false");
                        repository.save(other);
                    }
                }
            }

            return repository.save(role);
        } else {
            throw new RuntimeException("Role not found with id " + id);
        }
    }



    @Override
    public void deleteRole(Long id) {
        repository.deleteById(id);
    }
}