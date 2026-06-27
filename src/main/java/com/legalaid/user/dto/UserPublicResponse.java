package com.legalaid.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class UserPublicResponse {
    private UUID id;
    private String name;
    private String avatarUrl;
    private String role;
}
