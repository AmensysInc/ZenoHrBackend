package com.application.employee.service.repositories;
import com.application.employee.service.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface UserRepository extends JpaRepository<User,Integer> {
    Optional<User> findByEmail(String email);
    User findFirstByEmail(String email);

    User findById(String id);



}
