package com.bm.wschat.feature.auth.service;

import com.bm.wschat.feature.auth.dto.request.AuthRequest;
import com.bm.wschat.feature.auth.dto.request.RefreshTokenRequest;
import com.bm.wschat.feature.auth.dto.response.AuthResponse;
import com.bm.wschat.feature.auth.mapper.AuthMapper;
import com.bm.wschat.feature.auth.model.RefreshToken;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.shared.exception.InvalidRefreshTokenException;
import com.bm.wschat.shared.security.events.UserLoginEvent;
import com.bm.wschat.shared.security.jwt.JwtService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuthMapper authMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public AuthResponse login(@NotNull AuthRequest request) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = (User) Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
        if (user == null) {
            throw new IllegalStateException("Не удалось кастовать юзера из контекста");
        }
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        eventPublisher.publishEvent(new UserLoginEvent(user.getId()));

        return new AuthResponse(
                accessToken,
                refreshToken.getToken(),
                jwtService.getJwtExpiration(),
                authMapper.toAuthResponse(user));
    }

    @Transactional
    public AuthResponse refreshToken(@NotNull RefreshTokenRequest request) {
        return refreshTokenService.findByToken(request.refreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    // The old refresh token is valid, now rotate it.
                    // Delete the old one to prevent reuse.
                    refreshTokenService.deleteByToken(request.refreshToken());

                    // Create new tokens
                    String newAccessToken = jwtService.generateToken(user);
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(user.getId());

                    return new AuthResponse(
                            newAccessToken,
                            newRefreshToken.getToken(),
                            jwtService.getJwtExpiration(),
                            authMapper.toAuthResponse(user));
                })
                .orElseThrow(() -> new InvalidRefreshTokenException("Не валдиный рефреш токен"));
    }
}
