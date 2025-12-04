package com.bm.wschat.feature.auth.service;

import com.bm.wschat.feature.auth.dto.refresh.RefreshTokenRequest;
import com.bm.wschat.feature.auth.dto.request.AuthRequest;
import com.bm.wschat.feature.auth.dto.response.AuthResponse;
import com.bm.wschat.feature.auth.mapper.AuthMapper;
import com.bm.wschat.feature.auth.model.RefreshToken;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.service.UserService;
import com.bm.wschat.shared.exception.InvalidRefreshTokenException;
import com.bm.wschat.shared.security.jwt.JwtService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final AuthMapper authMapper;

    @Transactional
    public AuthResponse login(@NotNull AuthRequest request) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userService.findByUsername(request.username());
        String accessToken = jwtService.generateToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

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
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
    }
}
