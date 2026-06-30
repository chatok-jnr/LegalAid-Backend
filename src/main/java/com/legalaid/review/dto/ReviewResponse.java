package com.legalaid.review.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ReviewResponse {

    private UUID    id;
    private UUID    contractId;

    // Client info
    private UUID    clientId;
    private String  clientName;
    private String  clientAvatarUrl;

    // Lawyer info
    private UUID    lawyerId;

    // Review content
    private Integer rating;
    private String  comment;
    private String  reply;
    private Integer helpfulCount;
    private Boolean votedHelpful;    // whether current user voted helpful

    // Sub-items
    private List<AspectDto> aspects;
    private List<String>    tags;

    private Instant createdAt;
    private Instant updatedAt;

    @Getter
    @Builder
    public static class AspectDto {
        private String  aspect;
        private Integer rating;
    }
}