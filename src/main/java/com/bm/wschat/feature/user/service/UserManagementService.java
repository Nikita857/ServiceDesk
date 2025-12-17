package com.bm.wschat.feature.user.service;

import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.validation.PasswordValidator;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
    @CacheEvict(cacheNames = "users", allEntries = true)
    public User createUser(@NotNull String username, @NotNull String password,
                          String fio, String email, Set<String> roles, boolean active) {
        // Validate password
        if (!passwordValidator.isValid(password)) {
            throw new IllegalArgumentException(passwordValidator.getValidationMessage());
        }

        // Check if user already exists
        if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
            throw new DataIntegrityViolationException("Пользователь с логином '" + username + "' уже существует");
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
    @CacheEvict(cacheNames = "users", allEntries = true)
    public User updateUser(Long userId, String fio, String email, Set<String> roles, Boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        if (fio != null) user.setFio(fio);
        if (email != null) user.setEmail(email);
        if (roles != null) user.setRoles(roles);
        if (active != null) user.setActive(active);

        user.setUpdatedAt(Instant.now());

        User updatedUser = userRepository.save(user);
        log.info("Обновлен пользователь: {}", user.getUsername());
        return updatedUser;
    }

    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public void changePassword(Long userId, String newPassword) {
        // Validate password
        if (!passwordValidator.isValid(newPassword)) {
            throw new IllegalArgumentException(passwordValidator.getValidationMessage());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());

        userRepository.save(user);
        log.info("Пароль измене для пользователя: {}", user.getUsername());
    }

    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public void deleteUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new EntityNotFoundException("Пользователь не найден: " + userId);
        }
        
        userRepository.deleteById(userId);
        log.info("Удален пользователь: {}", userId);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "users", key = "#id")
    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + id));
    }
}