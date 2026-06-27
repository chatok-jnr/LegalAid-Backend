package com.legalaid.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    // All payouts for a lawyer
    List<Payout> findAllByLawyerIdOrderByRequestedAtDesc(UUID lawyerId);

    // Prevent requesting payout while one is already in progress
    boolean existsByLawyerIdAndStatusIn(UUID lawyerId, List<PayoutStatus> statuses);

    // Total paid out to lawyer — used for stats
    @Query("""
        SELECT COALESCE(SUM(p.amount), 0)
        FROM Payout p
        WHERE p.lawyerId = :lawyerId
        AND p.status = 'COMPLETED'
        """)
    BigDecimal sumCompletedPayoutsForLawyer(@Param("lawyerId") UUID lawyerId);
}