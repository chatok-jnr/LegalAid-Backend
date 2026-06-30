package com.legalaid.message.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnreadCountResponse {
    private long totalUnread;  // across all contracts
}