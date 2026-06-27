package com.legalaid.auth;

import com.legalaid.auth.dto.AuthResponse;
import com.legalaid.auth.dto.GoogleTokenRequest;
import com.legalaid.common.response.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── POST /api/auth/google ────────────────────────────────
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @Valid @RequestBody GoogleTokenRequest request,
            HttpServletResponse response) {

        AuthService.AuthResult result = authService.loginWithGoogle(request.getIdToken());
        setRefreshTokenCookie(response, result.refreshToken());

        ApiResponse<AuthResponse> body = ApiResponse.success(result.response());
        return ResponseEntity.ok(body);
    }

    // ── POST /api/auth/refresh ───────────────────────────────
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request);
        AuthService.AuthResult result = authService.refreshAccessToken(refreshToken);
        setRefreshTokenCookie(response, result.refreshToken());

        ApiResponse<AuthResponse> body = ApiResponse.success(result.response());
        return ResponseEntity.ok(body);
    }

    // ── POST /api/auth/logout ────────────────────────────────
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        clearRefreshTokenCookie(response);

        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }

    // ── GET /api/auth/me ─────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UUID>> me(
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<UUID> body = ApiResponse.success(userId);
        return ResponseEntity.ok(body);
    }

    // ── Cookie helpers ───────────────────────────────────────
    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refresh_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            throw new RuntimeException("No refresh token cookie found");
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> "refresh_token".equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Refresh token cookie missing"));
    }
}