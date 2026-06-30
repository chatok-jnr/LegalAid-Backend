package com.legalaid.dispute;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    // All disputes visible to a user (raised by them or they are a participant)
    @Query("""
        SELECT d FROM Dispute d
        JOIN Contract c ON c.id = d.contractId
        WHERE d.deletedAt IS NULL
        AND (d.raisedBy = :userId
             OR c.clientId = :userId
             OR c.lawyerId = :userId)
        ORDER BY d.createdAt DESC
        """)
    List<Dispute> findAllVisibleToUser(@Param("userId") UUID userId);

    // Single dispute with access check
    Optional<Dispute> findByIdAndDeletedAtIsNull(UUID id);

    // Prevent duplicate open disputes on same contract
    boolean existsByContractIdAndStatusIn(
            UUID contractId, List<DisputeStatus> statuses);

    // Admin — all disputes paginated
    Page<Dispute> findAllByDeletedAtIsNullOrderByCreatedAtDesc(Pageable pageable);

    // Admin — filter by status
    Page<Dispute> findAllByStatusAndDeletedAtIsNull(
            DisputeStatus status, Pageable pageable);
}