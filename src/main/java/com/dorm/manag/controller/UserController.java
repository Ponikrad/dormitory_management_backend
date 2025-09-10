package com.dorm.manag.controller;

import com.dorm.manag.entity.User;
import com.dorm.manag.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> user = userService.findByUsername(username);

            if (user.isPresent()) {
                return ResponseEntity.ok(createUserResponse(user.get()));
            }

            Map<String, String> response = new HashMap<>();
            response.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error retrieving current user: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve user");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateCurrentUser(@RequestBody User userDetails, Authentication authentication) {
        try {
            String username = authentication.getName();
            User currentUser = userService.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            User updatedUser = userService.updateUser(currentUser.getId(), userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User updated successfully");
            response.put("user", createUserResponse(updatedUser));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update user");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getAllStudents() {
        try {
            List<User> students = userService.findAllStudents();
            List<Map<String, Object>> studentResponses = students.stream()
                    .map(this::createUserResponse)
                    .toList();

            return ResponseEntity.ok(studentResponses);
        } catch (Exception e) {
            log.error("Error retrieving students: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve students");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTIONIST')")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Optional<User> user = userService.findById(id);

            if (user.isPresent()) {
                return ResponseEntity.ok(createUserResponse(user.get()));
            }

            Map<String, String> response = new HashMap<>();
            response.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            log.error("Error retrieving user {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve user");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        try {
            User updatedUser = userService.updateUser(id, userDetails);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "User updated successfully");
            response.put("user", createUserResponse(updatedUser));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating user {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to update user");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deactivateUser(@PathVariable Long id) {
        try {
            userService.deactivateUser(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "User deactivated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deactivating user {}: {}", id, e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to deactivate user");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getUserStats() {
        try {
            long studentsCount = userService.getStudentsCount();
            List<User> activeUsers = userService.findActiveUsers();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalStudents", studentsCount);
            stats.put("activeUsers", activeUsers.size());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error retrieving user statistics: {}", e.getMessage());
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to retrieve statistics");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("phoneNumber", user.getPhoneNumber());
        userInfo.put("roomNumber", user.getRoomNumber());
        userInfo.put("role", user.getRole().name());
        userInfo.put("active", user.isActive());
        userInfo.put("createdAt", user.getCreatedAt());

        return userInfo;
    }
}