package com.legalaid.case_.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CaseResponse {
    private UUID        id;
    private UUID        clientId;
    private UUID        lawyerId;
    private String      title;
    private String      category;
    private String      court;
    private String      description;
    private String      status;
    private String      priority;
    private LocalDate   nextHearingDate;
    private LocalDate   filedDate;
    private List<String>            tags;
    private List<MilestoneDto>      milestones;
    private List<AccessDto>         accessList;
    private Instant     createdAt;
    private Instant     updatedAt;

    @Getter
    @Builder
    public static class MilestoneDto {
        private UUID id;
        private String label;
        private Boolean isDone;
        private LocalDate dueDate;
        private Instant completedAt;
        private Integer sortOrder;
    }

    @Getter
    @Builder
    public static class AccessDto{
        private UUID userId;
        private String role;
        private UUID invitedBy;
        private Instant grantedAt;
    }
}
