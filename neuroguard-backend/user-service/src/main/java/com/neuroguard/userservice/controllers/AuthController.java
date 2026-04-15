package com.neuroguard.userservice.controllers;

import com.neuroguard.userservice.entities.PasswordResetToken;
import com.neuroguard.userservice.entities.User;
import com.neuroguard.userservice.repositories.PasswordResetTokenRepository;
import com.neuroguard.userservice.security.JwtUtils;
import com.neuroguard.userservice.services.EmailService;
import com.neuroguard.userservice.services.PasswordResetService;
import com.neuroguard.userservice.services.UserService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.neuroguard.userservice.dto.GoogleLoginRequest;
import com.neuroguard.userservice.entities.Role;
import org.springframework.beans.factory.annotation.Value;
import jakarta.validation.Valid;
import java.util.Collections;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Value("${app.google.client-id:550789921754-tdpg2nso52gvhr2mgdhk0ra01hk79kt8.apps.googleusercontent.com}")
    private String googleClientId;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        String token = userService.loginUser(loginRequest.getUsername(), loginRequest.getPassword());

        if (token != null) {
            userService.updateLastSeen(loginRequest.getUsername());
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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        Map<String, String> response = new HashMap<>();
        
        try {
            log.info("Received forgot password request for email: {}", request.getEmail());
            
            // Basic validation
            if (request.getEmail() == null || !isValidEmail(request.getEmail())) {
                response.put("message", "Valid email address is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Delegate and return success
            String resultMessage = passwordResetService.processForgotPassword(request.getEmail());
            response.put("message", resultMessage);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error in forgot password controller: ", e);
            response.put("message", "An error occurred while processing your request. Please try again.");
            response.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        Map<String, String> response = new HashMap<>();
        
        try {
            // Basic validation
            if (request.getToken() == null || request.getToken().trim().isEmpty()) {
                response.put("message", "Reset token is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
                response.put("message", "Password must be at least 6 characters long");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                response.put("message", "Passwords do not match");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
            // Delegate to service (this will handle the transaction and logic)
            passwordResetService.completePasswordReset(request.getToken().trim(), request.getNewPassword());
            
            response.put("message", "Password has been reset successfully. You can now login with your new password.");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error during password reset: ", e);
            response.put("message", "Failed to reset password: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest googleRequest) {
        try {
            log.info("Received Google login request");
            
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleRequest.getIdToken());
            if (idToken != null) {
                Payload payload = idToken.getPayload();

                // Get user information from payload
                String email = payload.getEmail();
                String firstName = (String) payload.get("given_name");
                String lastName = (String) payload.get("family_name");
                
                log.info("Google verification successful for email: {}", email);

                // Check if user exists
                Optional<User> userOpt = userService.findUserByEmail(email);
                User user;
                
                if (userOpt.isPresent()) {
                    user = userOpt.get();
                    log.info("Existing user found for Google login: {}", user.getUsername());
                    
                    // Generate JWT for existing user
                    String token = jwtUtils.generateJwtToken(user.getUsername(), user.getRole().name(), user.getId());
                    Map<String, Object> response = new HashMap<>();
                    response.put("token", token);
                    response.put("message", "Google login successful");
                    return ResponseEntity.ok(response);
                } else {
                    // New user - return indicator so frontend can ask for role
                    log.info("New Google user detected (email: {}). Requesting role selection.", email);
                    Map<String, Object> response = new HashMap<>();
                    response.put("newUser", true);
                    response.put("email", email);
                    response.put("firstName", firstName != null ? firstName : "");
                    response.put("lastName", lastName != null ? lastName : "");
                    return ResponseEntity.ok(response);
                }
            } else {
                log.warn("Invalid ID token received from Google");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message", "Invalid Google ID token"));
            }
        } catch (Exception e) {
            log.error("CRITICAL error during Google authentication: type={}, message={}", e.getClass().getName(), e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("message", "Google authentication failed: " + e.getMessage());
            error.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/google/complete")
    public ResponseEntity<?> googleComplete(@Valid @RequestBody GoogleLoginRequest googleRequest) {
        try {
            log.info("Completing Google registration");
            
            if (googleRequest.getRole() == null || googleRequest.getRole().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("message", "Role selection is required");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(googleRequest.getIdToken());
            if (idToken != null) {
                Payload payload = idToken.getPayload();
                String email = payload.getEmail();

                // Check again if exists (concurrency safety)
                Optional<User> userOpt = userService.findUserByEmail(email);
                if (userOpt.isPresent()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.singletonMap("message", "User already exists"));
                }

                // Create user
                User user = new User();
                user.setEmail(email);
                user.setFirstName((String) payload.get("given_name"));
                user.setLastName((String) payload.get("family_name"));
                user.setUsername(email.split("@")[0] + "_" + System.currentTimeMillis() % 1000);
                user.setRole(Role.valueOf(googleRequest.getRole().toUpperCase()));
                
                User savedUser = userService.registerGoogleUser(user);

                // Generate JWT
                String token = jwtUtils.generateJwtToken(savedUser.getUsername(), savedUser.getRole().name(), savedUser.getId());
                
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("message", "Google registration completed");
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Collections.singletonMap("message", "Invalid token"));
        } catch (Exception e) {
            log.error("Error in googleComplete: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.singletonMap("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestHeader(value = "Authorization", required = false) String token) {
        Map<String, String> response = new HashMap<>();

        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);

            // Validate the token before invalidating
            if (jwtUtils.validateJwtToken(jwt)) {
                String username = jwtUtils.getUsernameFromJwtToken(jwt);
                userService.clearLastSeen(username);
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

    @Getter
    @Setter
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        private String email;
    }

    @Getter
    @Setter
    public static class ResetPasswordRequest {
        @NotBlank(message = "Reset token is required")
        private String token;
        
        @NotBlank(message = "New password is required")
        private String newPassword;
        
        @NotBlank(message = "Password confirmation is required")
        private String confirmPassword;
    }

    // Helper method to validate email format
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }

}