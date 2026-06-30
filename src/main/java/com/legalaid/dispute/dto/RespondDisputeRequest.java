package com.legalaid.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RespondDisputeRequest {

    @NotBlank(message = "Response is required")
    @Size(max = 3000, message = "Response too long")
    private String response;
}