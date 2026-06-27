package com.legalaid.lawyer.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class LawyerStatsResponse {

    private BigDecimal  totalEarnings;      // wired when payment package is built
    private BigDecimal  pendingPayout;      // wired when payment package is built
    private BigDecimal  rating;
    private Integer     reviewCount;
    private Integer     totalCases;         // wired when case package is built
    private Integer     activeCases;        // wired when case package is built
    private Integer     completedContracts; // wired when contract package is built
}