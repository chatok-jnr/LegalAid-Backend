package com.legalaid.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PlatformStatsResponse {

    // Users
    private long totalUsers;
    private long totalClients;
    private long totalLawyers;
    private long totalAdmins;

    // Lawyers
    private long pendingVerifications;
    private long approvedLawyers;

    // Services
    private long totalServices;
    private long activeServices;

    // Contracts
    private long totalContracts;
    private long activeContracts;
    private long completedContracts;
    private long disputedContracts;
    private long cancelledContracts;

    // Payments
    private long        totalPayments;
    private long        pendingPayments;
    private BigDecimal  totalRevenue;        // sum of platform fees collected
    private BigDecimal  totalPayoutsPending; // sum of REQUESTED payouts

    // Disputes
    private long openDisputes;
    private long resolvedDisputes;
}