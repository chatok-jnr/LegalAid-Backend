package com.legalaid.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DisputeMessageRequest {

    @NotBlank(message = "Message text is required")
    @Size(max = 3000, message = "Message too long")
    private String text;
}