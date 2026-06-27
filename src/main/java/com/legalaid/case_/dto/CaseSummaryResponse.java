package com.legalaid.case_.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class CaseSummaryResponse {

    private UUID        id;
    private UUID        clientId;
    private UUID        lawyerId;
    private String      title;
    private String      category;
    private String      court;
    private String      status;
    private String      priority;
    private LocalDate   nextHearingDate;
    private LocalDate   filedDate;
    private List<String> tags;
    private Instant     createdAt;
    private Instant     updatedAt;
}