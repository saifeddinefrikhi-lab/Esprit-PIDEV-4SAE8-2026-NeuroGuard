package com.neuroguard.userservice.controllers;

import com.neuroguard.userservice.entities.User;
import com.neuroguard.userservice.security.JwtUtils;
import com.neuroguard.userservice.services.UserService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String token = userService.loginUser(loginRequest.getUsername(), loginRequest.getPassword());

        if (token != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        }

        Map<String, String> error = new HashMap<>();
        error.put("message", "Invalid credentials");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        String result = userService.registerUser(user);
        Map<String, String> response = new HashMap<>();
        response.put("message", result);

        if (result.contains("successfully")) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, String> response = new HashMap<>();

        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);

            // Validate the token before invalidating
            if (jwtUtils.validateJwtToken(jwt)) {
                jwtUtils.invalidateToken(jwt);
                SecurityContextHolder.clearContext();
                response.put("message", "User logged out successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        }

        response.put("message", "No active session to logout");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @Getter
    @Setter
    public static class LoginRequest {
        private String username;
        private String password;
    }

}