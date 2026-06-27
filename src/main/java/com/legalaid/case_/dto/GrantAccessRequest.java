package com.legalaid.case_.dto;

import com.legalaid.case_.enums.CaseAccessRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class GrantAccessRequest {
    @NotNull(message = "User ID is required")
    private UUID userId;

    private CaseAccessRole role;
}
