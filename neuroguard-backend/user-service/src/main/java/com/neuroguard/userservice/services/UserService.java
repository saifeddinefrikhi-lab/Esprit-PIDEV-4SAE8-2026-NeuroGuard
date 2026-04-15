package com.neuroguard.userservice.services;

import com.neuroguard.userservice.dto.CreateUserRequest;
import com.neuroguard.userservice.dto.UpdateUserRequest;
import com.neuroguard.userservice.dto.UserDto;
import com.neuroguard.userservice.dto.UserStatsDto;
import com.neuroguard.userservice.entities.Role;
import com.neuroguard.userservice.entities.User;
import com.neuroguard.userservice.security.JwtUtils;
import com.neuroguard.userservice.repositories.PasswordResetTokenRepository;
import com.neuroguard.userservice.repositories.UserRepository;
import com.neuroguard.userservice.services.PasswordResetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PasswordResetService passwordResetService;

    // Register a new user
    public String registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return "User already exists!";
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            return "Username already exists!";
        }
        if (user.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        userRepository.save(user);
        return "User registered successfully!";
    }

    // Register a new user from Google Login (handles password null)
    public User registerGoogleUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        user.setPassword(null); // Explicitly ensure no password
        return userRepository.save(user);
    }

    public String loginUser(String username, String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return jwtUtils.generateJwtToken(user.getUsername(), user.getRole().name(), user.getId());
        }
        return null;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Implement logic to load user by username
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        // Convert User entity to UserDetails (you may need to implement a custom UserDetails class)
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(String.valueOf(user.getRole()))
                .build();
    }

    public void updateLastSeen(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastSeen(java.time.LocalDateTime.now());
            userRepository.save(user);
        });
    }

    public void clearLastSeen(String username) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastSeen(null);
            userRepository.save(user);
        });
    }

    // In UserService.java

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public UserDto createUser(CreateUserRequest request) {
        // Validate uniqueness
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }
        User user = new User();
        // map fields
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setGender(request.getGender());
        user.setAge(request.getAge());
        user.setRole(Role.valueOf(request.getRole().toUpperCase()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        log.info("Creating new user via Admin: {}", request.getEmail());
        User saved = userRepository.saveAndFlush(user);
        
        // Trigger invitation email via password reset link (Exactly like Forgot Password feature)
        try {
            String result = passwordResetService.processForgotPassword(request.getEmail());
            log.info("Invitation result for {}: {}", request.getEmail(), result);
        } catch (Exception e) {
            log.error("CRITICAL: Failed to send invitation email for {}", request.getEmail(), e);
        }
        
        return convertToDto(saved);
    }

    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        // update only non-null fields
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getUsername() != null) {
            // check uniqueness if changed
            if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
                throw new RuntimeException("Username already exists");
            }
            user.setUsername(request.getUsername());
        }
        if (request.getEmail() != null) {
            if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already exists");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getAge() != null) user.setAge(request.getAge());
        if (request.getRole() != null) user.setRole(Role.valueOf(request.getRole().toUpperCase()));
        if (request.getPassword() != null) user.setPassword(passwordEncoder.encode(request.getPassword()));
        User updated = userRepository.save(user);
        return convertToDto(updated);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // 1. Explicitly delete all dependencies (tokens)
        passwordResetTokenRepository.deleteAllTokensByUser(user);
        
        // 2. Delete the user
        userRepository.delete(user);
    }

    // Helper conversion (already exists in UserController, can be moved to service)
    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setRole(user.getRole().name());
        
        // Check if user has been active in the last 15 minutes
        if (user.getLastSeen() != null) {
            java.time.LocalDateTime fifteenMinutesAgo = java.time.LocalDateTime.now().minusMinutes(15);
            dto.setConnected(user.getLastSeen().isAfter(fifteenMinutesAgo));
        } else {
            dto.setConnected(false);
        }
        
        return dto;
    }

    public UserStatsDto getStats() {
        long patients = userRepository.countByRole(Role.PATIENT);
        long providers = userRepository.countByRole(Role.PROVIDER);
        long caregivers = userRepository.countByRole(Role.CAREGIVER);
        long admins = userRepository.countByRole(Role.ADMIN);
        long total = userRepository.count();
        return new UserStatsDto(total, patients, providers, caregivers, admins);
    }

    // Find user by email (for password reset)
    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    // Update user password (for password reset)
    public void updateUserPassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}