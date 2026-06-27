package com.legalaid.contract.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class ContractDetailResponse {

    private UUID        id;
    private UUID        serviceId;
    private UUID        clientId;
    private UUID        lawyerId;
    private UUID        caseId;

    // Lawyer info
    private String      lawyerName;
    private String      lawyerAvatarUrl;

    // Client info
    private String      clientName;
    private String      clientAvatarUrl;

    // Snapshotted service values
    private String      serviceTitle;
    private BigDecimal  amount;
    private BigDecimal  platformFee;
    private Integer     deliveryDays;
    private Integer     revisions;

    // Status
    private String      status;
    private String      escrowStatus;
    private String      clientNote;

    private List<MilestoneDto> milestones;

    private Instant     createdAt;
    private Instant     updatedAt;

    @Getter
    @Builder
    public static class MilestoneDto {
        private UUID      id;
        private String    label;
        private Boolean   isDone;
        private LocalDate dueDate;
        private Instant   completedAt;
        private Integer   sortOrder;
    }
}