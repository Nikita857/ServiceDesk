package com.bm.wschat.feature.auth.service;


import com.bm.wschat.feature.auth.model.RefreshToken;
import com.bm.wschat.feature.auth.repository.RefreshTokenRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.exception.ExpiredTokenException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${jwt.refresh-token.expiration-ms:604800000}") // Default to 7 days if not set
    private Long refreshTokenDurationMs;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

//    TODO проверить реализацию удаления токена, миграцию V12, и сущности User и RefreshToken

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь с не найден: " + userId));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenDurationMs));
        refreshToken.setToken(UUID.randomUUID().toString());

        log.info("Удаляем старый рефреш токен, перед созданием нового");
        user.setRefreshToken(refreshToken);

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpiration(@NotNull RefreshToken token) {
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.findByToken(token.getToken())
                    .ifPresent(refreshToken -> {
                        User user = refreshToken.getUser();
                        // Hibernate удалит токен автоматически через orphanRemoval
                        user.setRefreshToken(null);
                        userRepository.save(user);
                    });

            throw new ExpiredTokenException(
                    "Рефреш токен просрочен, пожалуйста войдите снова"
            );
        }
        return token;
    }

    @Transactional
    public void deleteByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(
                refreshToken -> {
                    User user = refreshToken.getUser();
                    user.setRefreshToken(null);
                    userRepository.save(user);
                }
        );
    }

    @Transactional
    public void deleteByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Пользователь не найден: " + userId));
//        Передаем null так как установляена связь @OneToOne User - RefreshToken с orphanRemoval и Cascade type ALL
        user.setRefreshToken(null);
        userRepository.save(user);
    }
}