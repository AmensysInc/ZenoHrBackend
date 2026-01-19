package com.application.employee.service.repositories;

import com.application.employee.service.entities.UserCompanyRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UserCompanyRoleRepository extends JpaRepository<UserCompanyRole, Long> {
    List<UserCompanyRole> findByUserId(String userId);
    List<UserCompanyRole> findByCompanyId(Integer companyId);
}
