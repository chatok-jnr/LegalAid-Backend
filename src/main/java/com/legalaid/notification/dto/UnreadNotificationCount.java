package com.legalaid.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnreadNotificationCount {
    private long unreadCount;
}