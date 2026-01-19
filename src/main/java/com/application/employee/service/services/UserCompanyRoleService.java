package com.application.employee.service.services;

import com.application.employee.service.entities.UserCompanyRole;
import java.util.List;
import java.util.Optional;

public interface UserCompanyRoleService {
    UserCompanyRole saveUserCompanyRole(UserCompanyRole role);
    List<UserCompanyRole> getAllRoles();
    Optional<UserCompanyRole> getRoleById(Long id);
    List<UserCompanyRole> getRolesByUserId(String userId);
    List<UserCompanyRole> getRolesByCompanyId(Integer companyId);
    UserCompanyRole updateUserCompanyRole(Long id, UserCompanyRole updatedRole);
    void deleteRole(Long id);
}
