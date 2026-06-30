package com.legalaid.search.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class SearchResponse {

    private String                   query;
    private String                   type;         // ALL, LAWYER, SERVICE
    private long                     totalResults;
    private int                      page;
    private int                      size;
    private int                      totalPages;
    private List<SearchResultResponse> results;
}