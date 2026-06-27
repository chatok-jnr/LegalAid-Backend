package com.legalaid.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoogleTokenRequest {
    @NotBlank(message = "Google ID token is required")
    private String idToken; // Sent from frontend after google sign-in
}
