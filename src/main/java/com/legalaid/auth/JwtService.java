package com.legalaid.auth;

import com.legalaid.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtConfig jwtConfig;

    // ── Signing key ──────────────────────────────────────────
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(
                jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    // ── Generate ─────────────────────────────────────────────
    public String generateAccessToken(UUID userId, String role) {
        return buildToken(userId, role, jwtConfig.getAccessTokenExpiry(), "access");
    }

    public String generateRefreshToken(UUID userId, String role) {
        return buildToken(userId, role, jwtConfig.getRefreshTokenExpiry(), "refresh");
    }

    private String buildToken(UUID userId, String role, long expiry, String tokenType) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiry))
                .signWith(getSigningKey())
                .compact();
    }

    // ── Parse — only called internally after isTokenValid() ──
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // ── Safe extract — returns null on any failure ───────────
    // All public methods go through this instead of extractAllClaims directly
    private Claims safeExtract(String token) {
        try {
            return extractAllClaims(token);
        } catch (Exception e) {
            return null;
        }
    }

    public UUID extractUserId(String token) {
        Claims claims = safeExtract(token);
        if (claims == null) throw new RuntimeException("Invalid token");
        return UUID.fromString(claims.getSubject());
    }

    public String extractRole(String token) {
        Claims claims = safeExtract(token);
        if (claims == null) throw new RuntimeException("Invalid token");
        return claims.get("role", String.class);
    }

    private String extractTokenType(String token) {
        Claims claims = safeExtract(token);
        if (claims == null) return null;
        return claims.get("type", String.class);
    }

    // ── Validate ─────────────────────────────────────────────
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractAllClaims(token);
            return claims != null && !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    // Safe — returns false instead of throwing on bad input
    public boolean isAccessToken(String token) {
        String type = extractTokenType(token);
        return "access".equals(type);
    }

    // Safe — returns false instead of throwing on bad input
    public boolean isRefreshToken(String token) {
        String type = extractTokenType(token);
        return "refresh".equals(type);
    }
}