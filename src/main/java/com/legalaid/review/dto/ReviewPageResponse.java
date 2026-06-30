package com.legalaid.review.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReviewPageResponse {

    private List<ReviewResponse> reviews;
    private long    totalElements;
    private int     totalPages;
    private int     page;
    private int     size;
    private double  averageRating;
    private long    totalReviews;
}