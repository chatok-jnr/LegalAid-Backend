package com.legalaid.message.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendMessageRequest {

    @NotBlank(message = "Message text is required")
    @Size(max = 5000, message = "Message too long")
    private String text;

    // Optional — Cloudinary URL after frontend uploads the file
    private String attachmentUrl;

    private String attachmentName;
}