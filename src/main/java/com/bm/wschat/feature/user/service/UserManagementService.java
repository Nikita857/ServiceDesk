package com.bm.wschat.feature.user.service;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.validation.PasswordValidator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordValidator passwordValidator;

    @Transactional
    public User createUser(@NotNull String username, @NotNull String password,
                          String fio, String email, Set<String> roles, boolean active) {
        // Validate password
        if (!passwordValidator.isValid(password)) {
            throw new IllegalArgumentException(passwordValidator.getValidationMessage());
        }

        // Check if user already exists
        if (userRepository.findByUsername(username).isPresent()) {
            throw new DataIntegrityViolationException("User with username '" + username + "' already exists");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .fio(fio)
                .email(email)
                .specialist(false) // Default to false
                .roles(roles != null ? roles : Set.of("ROLE_USER"))
                .active(active)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("Created user: {}", username);
        return savedUser;
    }

    @Transactional
    public User updateUser(Long userId, String fio, String email, Set<String> roles, Boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        if (fio != null) user.setFio(fio);
        if (email != null) user.setEmail(email);
        if (roles != null) user.setRoles(roles);
        if (active != null) user.setActive(active);

        user.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(user);
        log.info("Updated user: {}", user.getUsername());
        return updatedUser;
    }

    @Transactional
    public void changePassword(Long userId, String newPassword) {
        // Validate password
        if (!passwordValidator.isValid(newPassword)) {
            throw new IllegalArgumentException(passwordValidator.getValidationMessage());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }

    @Transactional
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("User not found with id: " + userId);
        }
        
        userRepository.deleteById(userId);
        log.info("Deleted user with id: {}", userId);
    }

    @Transactional(readOnly = true)
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }
}