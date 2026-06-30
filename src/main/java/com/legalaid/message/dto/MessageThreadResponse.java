package com.legalaid.message.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class MessageThreadResponse {

    private UUID                contractId;
    private long                totalMessages;
    private long                unreadCount;
    private List<MessageResponse> messages;

    // Pagination info
    private int     page;
    private int     size;
    private int     totalPages;
}