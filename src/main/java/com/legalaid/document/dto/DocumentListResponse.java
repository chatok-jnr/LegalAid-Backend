package com.legalaid.document.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DocumentListResponse {

    private List<DocumentResponse> documents;
    private List<String>           folders;      // distinct folder names for sidebar
    private long                   totalCount;
}