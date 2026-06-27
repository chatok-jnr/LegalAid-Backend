package com.legalaid.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.legalaid.auth.dto.AuthResponse;
import com.legalaid.user.User;
import com.legalaid.user.UserRepository;
import com.legalaid.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock private UserRepository       userRepository;
    @Mock private JwtService           jwtService;
    @Mock private GoogleOAuthService   googleOAuthService;
    @Mock private GoogleIdToken.Payload googlePayload;  // fake Google response

    @InjectMocks
    private AuthService authService;

    private static final UUID   USER_ID      = UUID.randomUUID();
    private static final String GOOGLE_ID    = "google-123";
    private static final String EMAIL        = "test@legalaid.com";
    private static final String NAME         = "Test User";
    private static final String AVATAR       = "https://avatar.url/pic.jpg";
    private static final String ACCESS_TOKEN = "fake.access.token";
    private static final String REFRESH_TOKEN= "fake.refresh.token";

    @BeforeEach
    void setUp() {
        // fake Google payload — same shape as what Google sends
        lenient().when(googlePayload.getSubject()).thenReturn(GOOGLE_ID);
        lenient().when(googlePayload.getEmail()).thenReturn(EMAIL);
        lenient().when(googlePayload.get("name")).thenReturn(NAME);
        lenient().when(googlePayload.get("picture")).thenReturn(AVATAR);

        lenient().when(googleOAuthService.verifyToken(anyString())).thenReturn(googlePayload);
        lenient().when(jwtService.generateAccessToken(any(), anyString())).thenReturn(ACCESS_TOKEN);
        lenient().when(jwtService.generateRefreshToken(any(), anyString())).thenReturn(REFRESH_TOKEN);
    }

    // ── loginWithGoogle — new user ───────────────────────────

    @Test
    @DisplayName("New user should be created on first Google login")
    void newUserShouldBeCreatedOnFirstGoogleLogin() {
        // no existing user in DB
        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            // simulate DB assigning an ID on save
            return User.builder()
                    .id(USER_ID).name(u.getName()).email(u.getEmail())
                    .avatarUrl(u.getAvatarUrl()).googleId(u.getGoogleId())
                    .role(UserRole.CLIENT).build();
        });

        AuthService.AuthResult result = authService.loginWithGoogle("google-id-token");

        // user should have been saved once
        verify(userRepository, times(1)).save(any(User.class));

        AuthResponse response = result.response();
        assertThat(response.isNewUser()).isTrue();
        assertThat(response.getRole()).isEqualTo("CLIENT");
        assertThat(response.getEmail()).isEqualTo(EMAIL);
        assertThat(response.getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("New user should always get CLIENT role by default")
    void newUserShouldAlwaysGetClientRole() {
        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return User.builder()
                    .id(USER_ID).name(u.getName()).email(u.getEmail())
                    .role(u.getRole()).googleId(u.getGoogleId()).build();
        });

        AuthService.AuthResult result = authService.loginWithGoogle("google-id-token");

        assertThat(result.response().getRole()).isEqualTo("CLIENT");
    }

    // ── loginWithGoogle — existing user ──────────────────────

    @Test
    @DisplayName("Existing user should not be created again on second login")
    void existingUserShouldNotBeCreatedAgain() {
        User existingUser = buildUser(UserRole.CLIENT);
        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.of(existingUser));

        AuthService.AuthResult result = authService.loginWithGoogle("google-id-token");

        // save should NOT be called — user already exists
        verify(userRepository, never()).save(any(User.class));

        assertThat(result.response().isNewUser()).isFalse();
        assertThat(result.response().getEmail()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("User found by email should have google_id attached")
    void userFoundByEmailShouldHaveGoogleIdAttached() {
        // user registered before Google OAuth existed — no google_id yet
        User userWithoutGoogleId = User.builder()
                .id(USER_ID).name(NAME).email(EMAIL)
                .role(UserRole.CLIENT).googleId(null)  // no google_id
                .build();

        when(userRepository.findByGoogleId(GOOGLE_ID)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(userWithoutGoogleId));
        when(userRepository.save(any(User.class))).thenReturn(userWithoutGoogleId);

        authService.loginWithGoogle("google-id-token");

        // google_id should be attached and saved
        assertThat(userWithoutGoogleId.getGoogleId()).isEqualTo(GOOGLE_ID);
        verify(userRepository, times(1)).save(userWithoutGoogleId);
    }

    // ── refreshAccessToken ───────────────────────────────────

    @Test
    @DisplayName("Valid refresh token should return new access token")
    void validRefreshTokenShouldReturnNewAccessToken() {
        User user = buildUser(UserRole.CLIENT);

        when(jwtService.isTokenValid(REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.isRefreshToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(jwtService.extractRole(REFRESH_TOKEN)).thenReturn("CLIENT");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        AuthService.AuthResult result = authService.refreshAccessToken(REFRESH_TOKEN);

        assertThat(result.response().getAccessToken()).isEqualTo(ACCESS_TOKEN);
        assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
    }

    @Test
    @DisplayName("Invalid refresh token should throw exception")
    void invalidRefreshTokenShouldThrowException() {
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshAccessToken("bad-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    @DisplayName("Access token used as refresh token should throw exception")
    void accessTokenUsedAsRefreshShouldThrow() {
        when(jwtService.isTokenValid(ACCESS_TOKEN)).thenReturn(true);
        when(jwtService.isRefreshToken(ACCESS_TOKEN)).thenReturn(false); // wrong type

        assertThatThrownBy(() -> authService.refreshAccessToken(ACCESS_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    @DisplayName("Refresh token for deleted user should throw exception")
    void refreshTokenForDeletedUserShouldThrow() {
        when(jwtService.isTokenValid(REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.isRefreshToken(REFRESH_TOKEN)).thenReturn(true);
        when(jwtService.extractUserId(REFRESH_TOKEN)).thenReturn(USER_ID);
        when(jwtService.extractRole(REFRESH_TOKEN)).thenReturn("CLIENT");
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty()); // user gone

        assertThatThrownBy(() -> authService.refreshAccessToken(REFRESH_TOKEN))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    // ── Helper ───────────────────────────────────────────────
    private User buildUser(UserRole role) {
        return User.builder()
                .id(USER_ID).name(NAME).email(EMAIL)
                .avatarUrl(AVATAR).googleId(GOOGLE_ID)
                .role(role).build();
    }
}