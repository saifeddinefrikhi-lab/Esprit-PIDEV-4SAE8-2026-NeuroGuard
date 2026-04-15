package com.neuroguard.userservice.services;

import com.neuroguard.userservice.entities.User;
import com.neuroguard.userservice.security.JwtUtils;
import com.neuroguard.userservice.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    // Register a new user
    public String registerUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            return "User already exists!";
        }
        // Set username to email if not provided
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            user.setUsername(user.getEmail());
        }
        if (userRepository.existsByUsername(user.getUsername())) {
            return "Username already exists!";
        }
        user.setPassword(new BCryptPasswordEncoder().encode(user.getPassword()));  // Hash the password
        userRepository.save(user); // Save user to database
        return "User registered successfully!";
    }

    public String loginUser(String username, String password) {
        try {
            if (username == null || password == null || username.isBlank() || password.isBlank()) {
                return null;
            }

            User user;
            // Try to find by email first, then by username
            if (username.contains("@")) {
                user = userRepository.findByEmail(username.trim()).orElse(null);
            } else {
                user = userRepository.findByUsername(username.trim()).orElse(null);
            }

            if (user != null && new BCryptPasswordEncoder().matches(password, user.getPassword())) {
                return jwtUtils.generateJwtToken(user.getUsername(), user.getRole().name(), user.getId()); // pass ID
            }
        } catch (Exception exception) {
            System.err.println("[UserService] loginUser failed: " + exception.getMessage());
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

}