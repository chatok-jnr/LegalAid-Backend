package com.legalaid.dispute.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class DisputeResponse {

    private UUID        id;
    private UUID        contractId;
    private UUID        raisedBy;
    private String      raisedByName;
    private String      reason;
    private String      description;
    private BigDecimal  amount;
    private String      status;
    private String      priority;
    private Instant     resolutionDeadline;

    // Resolution
    private UUID        resolvedBy;
    private String      resolutionNote;
    private Instant     resolvedAt;

    // Thread
    private List<EvidenceDto>  evidence;
    private List<MessageDto>   messages;
    private List<TimelineDto>  timeline;

    private Instant     createdAt;
    private Instant     updatedAt;

    @Getter @Builder
    public static class EvidenceDto {
        private UUID    id;
        private UUID    uploadedBy;
        private String  uploadedByName;
        private String  url;
        private String  name;
        private Instant uploadedAt;
    }

    @Getter @Builder
    public static class MessageDto {
        private UUID    id;
        private UUID    senderId;
        private String  senderName;
        private String  text;
        private Instant createdAt;
    }

    @Getter @Builder
    public static class TimelineDto {
        private String  type;
        private String  text;
        private UUID    actorId;
        private Instant createdAt;
    }
}