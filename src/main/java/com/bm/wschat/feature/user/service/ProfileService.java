package com.bm.wschat.feature.user.service;

import com.bm.wschat.feature.ticket.repository.TicketRepository;
import com.bm.wschat.feature.user.dto.response.ProfileResponse;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.storage.MinioStorageService;
import com.bm.wschat.shared.validation.PasswordValidator;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.cache.annotation.CacheEvict;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.Set;

/**
 * Сервис для работы с личным кабинетом пользователя.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final PasswordValidator passwordValidator;
    private final PasswordEncoder passwordEncoder;
    private final MinioStorageService minioStorageService;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final long MAX_AVATAR_SIZE = 10 * 1024 * 1024; // 10MB в байтах

    /**
     * Получить профиль пользователя.
     * Для специалистов включает среднюю оценку.
     */
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        Double averageRating = null;
        Long ratedTicketsCount = null;

        if (user.isSpecialist()) {
            var ratingResult = ticketRepository.getAverageRatingBySpecialist(userId);
            if (ratingResult != null && !ratingResult.isEmpty()) {
                Object[] row = ratingResult.get(0);
                if (row[0] != null) {
                    averageRating = ((Number) row[0]).doubleValue();
                    ratedTicketsCount = ((Number) row[1]).longValue();
                }
            }
        }

        // Генерируем полный URL для аватара
        String avatarUrl = null;
        if (user.getAvatarUrl() != null) {
            avatarUrl = minioStorageService.generateDownloadUrl(
                    user.getAvatarUrl(),
                    minioStorageService.getBucket(MinioStorageService.BucketType.CHAT),
                    user.getAvatarUrl()); // используем fileKey как имя файла
        }

        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getFio(),
                user.getEmail(),
                user.getTelegramId(),
                avatarUrl,
                user.getRoles(),
                user.isSpecialist(),
                averageRating,
                ratedTicketsCount,
                user.getCreatedAt());
    }

    /**
     * Обновить профиль (ФИО, email).
     */
    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public ProfileResponse updateProfile(Long userId, String fio, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        if (fio != null) {
            user.setFio(fio);
        }
        if (email != null) {
            // Проверяем уникальность email
            if (userRepository.existsByEmailAndIdNot(email, userId)) {
                throw new IllegalArgumentException("Email уже используется другим пользователем");
            }
            user.setEmail(email);
        }

        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        log.info("Профиль обновлён: userId={}", userId);

        return getProfile(userId);
    }

    /**
     * Загрузить аватар.
     */
    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public String uploadAvatar(Long userId, MultipartFile file) {
        validateAvatarFile(file);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        // Удаляем старый аватар если есть
        if (user.getAvatarUrl() != null) {
            try {
                minioStorageService.deleteFile(user.getAvatarUrl(),
                        minioStorageService.getBucket(MinioStorageService.BucketType.CHAT));
            } catch (Exception e) {
                log.warn("Не удалось удалить старый аватар: {}", e.getMessage());
            }
        }

        // Загружаем файл в MinIO
        String fileKey = minioStorageService.uploadFile(file, MinioStorageService.BucketType.CHAT);

        user.setAvatarUrl(fileKey);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        log.info("Аватар загружен: userId={}, fileKey={}", userId, fileKey);
        return minioStorageService.generateDownloadUrl(fileKey,
                minioStorageService.getBucket(MinioStorageService.BucketType.CHAT), file.getOriginalFilename());
    }

    /**
     * Изменить пароль.
     */
    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public void changePassword(User user, String oldPassword, String newPassword) throws AccessDeniedException {
        // Validate password
        if (!passwordValidator.isValid(newPassword)) {
            throw new IllegalArgumentException(passwordValidator.getValidationMessage());
        }

        User foundedUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + user.getId()));

        if (foundedUser.getId() != user.getId()) {
            throw new AccessDeniedException("Доступ запрещен");
        }

        if (!passwordEncoder.matches(oldPassword, foundedUser.getPassword())) {
            throw new IllegalArgumentException("Пароли не совпадают");
        }

        foundedUser.setPassword(passwordEncoder.encode(newPassword));
        foundedUser.setUpdatedAt(Instant.now());

        userRepository.save(user);
        log.info("Пароль изменен для пользователя: {}", user.getUsername());
    }

    /**
     * Удалить аватар.
     */
    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public void deleteAvatar(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        if (user.getAvatarUrl() != null) {
            try {
                minioStorageService.deleteFile(user.getAvatarUrl(),
                        minioStorageService.getBucket(MinioStorageService.BucketType.CHAT));
            } catch (Exception e) {
                log.warn("Не удалось удалить аватар: {}", e.getMessage());
            }
            user.setAvatarUrl(null);
            user.setUpdatedAt(Instant.now());
            userRepository.save(user);
            log.info("Аватар удалён: userId={}", userId);
        }
    }

    /**
     * Обновить Telegram ID.
     */
    @Transactional
    @CacheEvict(cacheNames = "users", allEntries = true)
    public void updateTelegramId(Long userId, Long telegramId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));

        // Проверяем уникальность telegramId
        if (userRepository.existsByTelegramIdAndIdNot(telegramId, userId)) {
            throw new IllegalArgumentException("Этот Telegram ID уже привязан к другому аккаунту");
        }

        user.setTelegramId(telegramId);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        log.info("Telegram ID обновлён: userId={}, telegramId={}", userId, telegramId);
    }

    // === Private helpers ===

    private void validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не выбран");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("Максимальный размер аватара — 10 МБ");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Разрешены только изображения (JPEG, PNG, GIF, WebP)");
        }
    }
}
