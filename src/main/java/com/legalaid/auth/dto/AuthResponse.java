package com.legalaid.auth.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
public class AuthResponse {
    private UUID userId;
    private String name;
    private String email;
    private String avatarUrl;
    private String role;
    private boolean isNewUser; // frontend usees this to redirect to onboarding
    private String accessToken;
    // refresh token is not here - its send in httpOnly cookie
}
