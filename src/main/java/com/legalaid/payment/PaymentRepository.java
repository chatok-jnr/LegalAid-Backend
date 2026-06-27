package com.legalaid.payment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // All payments made by a client
    List<Payment> findAllByClientIdOrderByCreatedAtDesc(UUID clientId);

    // Payment for a specific contract
    Optional<Payment> findByContractId(UUID contractId);

    // Prevent duplicate payments for same contract
    boolean existsByContractIdAndStatusIn(UUID contractId, List<PaymentStatus> statuses);

    // Find by TxnID — used by admin to verify
    Optional<Payment> findByTxnId(String txnId);

    // Total released amount for a lawyer — used for stats
    // Sums all RELEASED payments across contracts belonging to a lawyer
    @Query("""
        SELECT COALESCE(SUM(p.amount - p.platformFee), 0)
        FROM Payment p
        JOIN Contract c ON c.id = p.contractId
        WHERE c.lawyerId = :lawyerUserId
        AND p.status = 'RELEASED'
        """)
    BigDecimal sumReleasedEarningsForLawyer(@Param("lawyerUserId") UUID lawyerUserId);

    // Total held (pending release) for a lawyer
    @Query("""
        SELECT COALESCE(SUM(p.amount - p.platformFee), 0)
        FROM Payment p
        JOIN Contract c ON c.id = p.contractId
        WHERE c.lawyerId = :lawyerUserId
        AND p.status = 'HELD'
        """)
    BigDecimal sumHeldEarningsForLawyer(@Param("lawyerUserId") UUID lawyerUserId);
}