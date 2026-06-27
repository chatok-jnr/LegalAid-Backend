package com.legalaid.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.legalaid.auth.dto.AuthResponse;
import com.legalaid.user.User;
import com.legalaid.user.UserRepository;
import com.legalaid.user.UserRole;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository     userRepository;   // Bug 1 fixed — uncommented
    private final JwtService         jwtService;
    private final GoogleOAuthService googleOAuthService;

    @Transactional
    public AuthResult loginWithGoogle(String idToken) {

        GoogleIdToken.Payload payload = googleOAuthService.verifyToken(idToken);

        String googleId  = payload.getSubject();
        String email     = payload.getEmail();
        String name      = (String) payload.get("name");
        String avatarUrl = (String) payload.get("picture"); // "picture" is Google's actual field name

        Optional<User> existing = userRepository.findByGoogleId(googleId);
        if (existing.isEmpty()) {
            existing = userRepository.findByEmail(email);
        }

        boolean isNewUser = existing.isEmpty();

        User user = existing.orElseGet(() -> {
            User newUser = User.builder()  // Bug 2 fixed — removed "new"
                    .name(name)
                    .email(email)
                    .avatarUrl(avatarUrl)
                    .googleId(googleId)
                    .role(UserRole.CLIENT)
                    .build();
            return userRepository.save(newUser);
        });

        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            userRepository.save(user);
        }

        String accessToken  = jwtService.generateAccessToken(user.getId(), user.getRole().name());  // Bug 3 fixed — getRle() → getRole()
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getRole().name());

        AuthResponse response = buildAuthResponse(user, accessToken, isNewUser);
        return new AuthResult(response, refreshToken);
    }

    public AuthResult refreshAccessToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken) || !jwtService.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        UUID   userId = jwtService.extractUserId(refreshToken);
        String role   = jwtService.extractRole(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken  = jwtService.generateAccessToken(userId, role);
        String newRefreshToken = jwtService.generateRefreshToken(userId, role); // Bug 4 fixed — was calling generateAccessToken

        AuthResponse response = buildAuthResponse(user, newAccessToken, false);
        return new AuthResult(response, newRefreshToken);
    }

    // Bug 5 fixed — renamed authResponse → response so AuthController's result.response() works
    public record AuthResult(AuthResponse response, String refreshToken) {}

    private AuthResponse buildAuthResponse(User user, String accessToken, boolean isNewUser) {
        return AuthResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .isNewUser(isNewUser)
                .accessToken(accessToken)  // Bug 6 fixed — accessToekn → accessToken
                .build();
    }
}