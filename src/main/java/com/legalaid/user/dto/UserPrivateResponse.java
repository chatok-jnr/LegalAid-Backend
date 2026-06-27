package com.legalaid.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class UserPrivateResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String address;
    private String avatarUrl;
    private String role;
    private Instant createdAt;
}
