package com.dorm.manag.service;

import com.dorm.manag.config.JwtTokenProvider;
import com.dorm.manag.dto.LoginRequest;
import com.dorm.manag.dto.RegisterRequest;
import com.dorm.manag.entity.Role;
import com.dorm.manag.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Map<String, Object> authenticateUser(LoginRequest loginRequest) {
        log.info("Authenticating user: {}", loginRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtTokenProvider.generateToken(authentication);

        User user = userService.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> response = new HashMap<>();
        response.put("token", jwt);
        response.put("tokenType", "Bearer");
        response.put("user", createUserResponse(user));

        return response;
    }

    @Transactional
    public Map<String, Object> registerUser(RegisterRequest registerRequest) {
        log.info("Registering new user: {}", registerRequest.getUsername());

        // Validate input
        validateRegisterRequest(registerRequest);

        // Create new user
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setRoomNumber(registerRequest.getRoomNumber());
        user.setRole(Role.STUDENT); // Default role for registration

        User savedUser = userService.createUser(user);

        // Auto-login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getUsername(),
                        registerRequest.getPassword()));

        String jwt = jwtTokenProvider.generateToken(authentication);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "User registered successfully");
        response.put("token", jwt);
        response.put("tokenType", "Bearer");
        response.put("user", createUserResponse(savedUser));

        return response;
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (userService.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }

        if (userService.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }

        if (request.getPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters long!");
        }
    }

    public Map<String, Object> getCurrentUserProfile(Authentication authentication) {
        String username = authentication.getName();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return createUserResponse(user);
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