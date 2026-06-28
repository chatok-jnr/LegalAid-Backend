package com.legalaid.document.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class DocumentResponse {

    private UUID    id;
    private UUID    ownerId;
    private UUID    uploadedBy;
    private UUID    caseId;
    private UUID    contractId;
    private String  name;
    private String  url;
    private String  cloudinaryId;
    private Long    fileSizeBytes;
    private String  mimeType;
    private String  folderName;
    private Boolean isStarred;
    private Boolean isShared;
    private Instant createdAt;
}