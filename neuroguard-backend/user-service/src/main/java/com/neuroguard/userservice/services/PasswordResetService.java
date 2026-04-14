package com.neuroguard.userservice.services;

import com.neuroguard.userservice.entities.PasswordResetToken;
import com.neuroguard.userservice.entities.User;
import com.neuroguard.userservice.repositories.PasswordResetTokenRepository;
import com.neuroguard.userservice.repositories.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
public class PasswordResetService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Transactional
    public void completePasswordReset(String token, String newPassword) {
        log.info("Completing password reset for token: {}", token);
        
        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            throw new RuntimeException("Invalid or expired reset token");
        }
        
        PasswordResetToken resetToken = tokenOpt.get();
        if (!resetToken.isValid()) {
            throw new RuntimeException("Invalid or expired reset token");
        }
        
        User user = resetToken.getUser();
        
        // 1. Update user password
        userService.updateUserPassword(user, newPassword);
        
        // 2. Mark token as used
        passwordResetTokenRepository.markTokenAsUsed(token);
        
        // 3. Clean up all tokens for this user
        passwordResetTokenRepository.deleteAllTokensByUser(user);
        
        log.info("Password reset completed successfully for user: {}", user.getEmail());
    }

    @Transactional
    public String processForgotPassword(String email) {
        log.info("Processing password reset request for email: {}", email);

        // 1. Find User
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.info("No user found with email: {} - returning generic success", email);
            return "If an account with that email exists, a password reset link has been sent.";
        }

        User user = userOpt.get();
        log.info("User identified: {} (ID: {})", user.getEmail(), user.getId());

        // 2. Manage Tokens
        String tokenToUse;
        
        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository
                .findValidTokenByUser(user, LocalDateTime.now());

        if (existingToken.isPresent()) {
            PasswordResetToken token = existingToken.get();
            // Re-use if created in the last 5 minutes to prevent spam but allow re-send
            if (token.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
                log.info("Re-using recently created token for user: {}", user.getEmail());
                tokenToUse = token.getToken();
            } else {
                // Token exists but is older than 5 mins, create a fresh one
                log.info("Creating fresh token for user: {}", user.getEmail());
                PasswordResetToken newToken = new PasswordResetToken(user);
                passwordResetTokenRepository.save(newToken);
                tokenToUse = newToken.getToken();
            }
        } else {
            // No valid token, create new
            log.info("No valid token exists. Creating new one for user: {}", user.getEmail());
            PasswordResetToken newToken = new PasswordResetToken(user);
            passwordResetTokenRepository.save(newToken);
            tokenToUse = newToken.getToken();
        }

        // 3. Hand off to Email Service (passing Strings only for thread safety)
        log.info("Handing off password reset email task to background thread for: {}", user.getEmail());
        emailService.sendPasswordResetEmail(
            user.getEmail(), 
            user.getFirstName(), 
            user.getLastName(), 
            tokenToUse
        );

        return "Password reset link has been sent to your email.";
    }
}
