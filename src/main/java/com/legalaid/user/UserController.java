package com.legalaid.user;

import com.legalaid.common.response.ApiResponse;
import com.legalaid.user.dto.UpdateUserRequest;
import com.legalaid.user.dto.UserPrivateResponse;
import com.legalaid.user.dto.UserPublicResponse;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    // ── GET /api/users/:id — anyone can view public profile ──
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserPublicResponse>> getPublicProfile(
            @PathVariable UUID id
    ) {
        ApiResponse<UserPublicResponse> body = ApiResponse.success(
                userService.getPublicProfile(id)
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/users/me — get own full profile ─────────────
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserPrivateResponse>> getMyProfile(
            @AuthenticationPrincipal UUID userId
    ) {
        ApiResponse<UserPrivateResponse> body = ApiResponse.success(
                userService.getMyProfile(userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/users/me — update own profile ───────────────
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserPrivateResponse>> updateMyProfile(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody UpdateUserRequest request
            ) {
        ApiResponse<UserPrivateResponse> body = ApiResponse.success(
                userService.updateMyProfile(userId, request)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/users/me/request-lawyer — request lawyer role ──
    // Role is NOT granted here. Starts the onboarding flow.
    @PutMapping("/me/request-lawyer")
    public ResponseEntity<ApiResponse<UserPrivateResponse>> requestLawyerRole(
            @AuthenticationPrincipal UUID userId) {
        ApiResponse<UserPrivateResponse> body = ApiResponse.success(
                userService.requestLawyerRole(userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── DELETE /api/users/me — soft delete account ───────────
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            @AuthenticationPrincipal UUID userId
    ) {
        userService.deleteMyAccount(userId);
        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }
}
