package com.legalaid.contract.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateContractRequest {

    @NotNull(message = "Service ID is required")
    private UUID serviceId;

    // Optional — links the contract to an existing case
    private UUID caseId;

    // Optional note from client to lawyer
    private String clientNote;
}