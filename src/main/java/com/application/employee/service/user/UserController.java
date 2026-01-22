package com.application.employee.service.user;

import com.application.employee.service.entities.Employee;
import com.application.employee.service.repositories.EmployeeRespository;
import com.application.employee.service.repositories.UserCompanyRoleRepository;
import com.application.employee.service.user.Role;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserAccountRepository userRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;

    @Autowired
    private EmployeeRespository employeeRespository;

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(user -> modelMapper.map(user, UserDto.class))
                .toList();
        return ResponseEntity.ok(users);
    }


    // GET user by ID
    @GetMapping("/{id}")
    public User getUserById(@PathVariable String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SADMIN', 'GROUP_ADMIN')")
    public ResponseEntity<String> deleteUser(@PathVariable String id) {
        try {
            // Check if user exists
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + id));

            // If user is a REPORTING_MANAGER, remove reportingManagerId from all employees
            if (user.getRole() == Role.REPORTING_MANAGER) {
                List<Employee> employees = employeeRespository.findAll();
                for (Employee employee : employees) {
                    if (id.equals(employee.getReportingManagerId())) {
                        employee.setReportingManagerId(null);
                        employeeRespository.save(employee);
                    }
                }
            }

            // Delete all UserCompanyRole entries for this user
            List<com.application.employee.service.entities.UserCompanyRole> userRoles = 
                    userCompanyRoleRepository.findByUserId(id);
            userCompanyRoleRepository.deleteAll(userRoles);

            // Delete the user
            userRepository.delete(user);

            return ResponseEntity.ok("User deleted successfully along with all associated roles and employee assignments");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting user: " + e.getMessage());
        }
    }
}
