package com.legalaid.auth;

import com.legalaid.config.JwtConfig;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class) // tells JUnit to user Mockito
@DisplayName("JwtService Tests")
public class JwtServiceTest {

    @Mock
    private JwtConfig jwtConfig; // fake JwtConfig - no spring context needed

    @InjectMocks
    private JwtService jwtService; // reaal JwtService with fake config injected

    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-for-hmac-sha";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String ROLE = "CLIENT";

    @BeforeEach
    void setUp() {
        // lenient() tells Mockito: "this stub may not be used by every test, that's fine"
        lenient().when(jwtConfig.getSecret()).thenReturn(SECRET);
        lenient().when(jwtConfig.getAccessTokenExpiry()).thenReturn(900000L);
        lenient().when(jwtConfig.getRefreshTokenExpiry()).thenReturn(604800000L);
    }

    // ── Access token tests ───────────────────────────────────

    @Test
    @DisplayName("Should generate a valid access token")
    void shouldGenerateValidAccessToken() {
        String token = jwtService.generateAccessToken(USER_ID, ROLE);

        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Access token should contain correct user ID")
    void accessTokenShouldContainCorrectUserId() {
        String token = jwtService.generateAccessToken(USER_ID, ROLE);

        UUID extractedId = jwtService.extractUserId(token);

        assertThat(extractedId).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("Access token should contain correct role")
    void accessTokenShouldContainCorrectRole() {
        String token = jwtService.generateAccessToken(USER_ID, ROLE);
        String extractedRole = jwtService.extractRole(token);
        assertThat(extractedRole).isEqualTo(ROLE);
    }

    @Test
    @DisplayName("Access token should be identified as access type")
    void accessTokenShouldBeIdentifiedAsAccessType() {
        String token = jwtService.generateAccessToken(USER_ID, ROLE);
        assertThat(jwtService.isAccessToken(token)).isTrue();
        assertThat(jwtService.isRefreshToken(token)).isFalse();
    }

    @Test
    @DisplayName("Valid access token should pass validation")
    void validAccessTokenShouldPassValidation() {
        String token = jwtService.generateAccessToken(USER_ID, ROLE);
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    // ── Refresh token tests ──────────────────────────────────
    @Test
    @DisplayName("Should genereate a valid refresh token")
    void shouldGenerateValidRefreshToken() {
        String token = jwtService.generateRefreshToken(USER_ID, ROLE);

        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("Refresh token should be identified as refresh type")
    void refreshTokenShouldBeIdentifiedAsRefreshType() {
        String token = jwtService.generateRefreshToken(USER_ID, ROLE);
        assertThat(jwtService.isRefreshToken(token)).isTrue();
        assertThat(jwtService.isAccessToken(token)).isFalse();
    }

    @Test
    @DisplayName("Refresh token should contain correct user ID")
    void refreshTokenShouldContainCorrectUserId() {
        String token = jwtService.generateRefreshToken(USER_ID, ROLE);
        UUID extractedId = jwtService.extractUserId(token);
        assertThat(extractedId).isEqualTo(USER_ID);
    }

    // ── Invalid token tests ──────────────────────────────────
    @Test
    @DisplayName("Tampered token should fail validation")
    void tamperedTokenShouldFailValidation() {
        String token = jwtService.generateRefreshToken(USER_ID, ROLE);
        String tamperedToken = token + "tampered";

        assertThat(jwtService.isTokenValid(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Expired token should fail validation")
    void expiredTokenShouldFailValidation() {
        // generate token with -1000ms expiry so it's born already expired
        Date now = new Date();
        String expiredToken = Jwts.builder()
                .subject(USER_ID.toString())
                .claim("role", ROLE)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() - 1000))  // expired 1 second ago
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThat(jwtService.isTokenValid(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("Random string should fail validation")
    void randomStringShouldFailValidation() {
        assertThat(jwtService.isRefreshToken("not.a.token")).isFalse();
    }

    @Test
    @DisplayName("Blank string should fail validation")
    void blankStringShouldFailValidation() {
        assertThat(jwtService.isTokenValid("")).isFalse();
    }

    @Test
    @DisplayName("Access token should not be usable as refresh token")
    void accessTokenShouldNotBeUsableAsRefreshToken() {
        String accessToken = jwtService.generateAccessToken(USER_ID, ROLE);
        assertThat(jwtService.isRefreshToken(accessToken)).isFalse();
    }
}
