package com.legalaid.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    @Size(min = 2, max = 120, message = "Name must be between 2 and 120 characters")
    private String name;

    @Size(max = 20, message = "Phone number too long")
    private String phone;

    @Size(max = 500, message = "Address too long")
    private String address;

    @Size(max = 500, message = "Avatar URL too long")
    private String avatarUrl;
}
