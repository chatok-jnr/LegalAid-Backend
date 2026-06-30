package com.legalaid.message.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class MessageResponse {

    private UUID    id;
    private UUID    contractId;
    private UUID    senderId;
    private String  senderName;
    private String  senderAvatarUrl;
    private String  text;
    private String  attachmentUrl;
    private String  attachmentName;
    private Boolean isRead;
    private Instant createdAt;
}