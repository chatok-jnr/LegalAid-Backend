package com.legalaid.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class NotificationResponse {

    private UUID    id;
    private String  type;
    private String  title;
    private String  body;
    private Boolean isRead;
    private String  actionUrl;
    private UUID    referenceId;
    private String  referenceType;
    private Instant createdAt;
}