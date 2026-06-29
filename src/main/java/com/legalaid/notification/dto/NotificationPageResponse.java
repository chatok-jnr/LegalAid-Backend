package com.legalaid.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class NotificationPageResponse {

    private List<NotificationResponse> notifications;
    private long    totalElements;
    private int     totalPages;
    private int     page;
    private int     size;
    private long    unreadCount;
}