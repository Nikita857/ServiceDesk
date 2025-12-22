package com.bm.wschat.shared.security.jwt;

import com.bm.wschat.feature.auth.service.CustomUserDetailsService;
import com.bm.wschat.feature.user.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final CustomUserDetailsService customUserDetailsService;

    // Error codes for frontend to distinguish error types
    public static final String ERROR_TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String ERROR_TOKEN_INVALID = "TOKEN_INVALID";
    public static final String ERROR_USER_NOT_FOUND = "USER_NOT_FOUND";
    public static final String ERROR_AUTH_FAILED = "AUTH_FAILED";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = authHeader.substring(7);

        try {
            String username = jwtService.extractUsername(jwt);

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                User user = (User) customUserDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, user)) {

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            user,
                            null,
                            user.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.debug("JWT token expired for request: {}", request.getRequestURI());
            sendUnauthorized(ERROR_TOKEN_EXPIRED, "JWT токен просрочен. Используйте refresh token для обновления.",
                    response);
        } catch (SignatureException | MalformedJwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            sendUnauthorized(ERROR_TOKEN_INVALID, "Неверный JWT токен", response);
        } catch (UsernameNotFoundException e) {
            log.warn("User not found for JWT: {}", e.getMessage());
            sendUnauthorized(ERROR_USER_NOT_FOUND, "Пользователь не найден", response);
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage(), e);
            sendUnauthorized(ERROR_AUTH_FAILED, "Ошибка аутентификации", response);
        }
    }

    private void sendUnauthorized(String errorCode, String message, HttpServletResponse response) throws IOException {
        var errorResponse = Map.of(
                "success", false,
                "errorCode", errorCode,
                "message", message);

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}