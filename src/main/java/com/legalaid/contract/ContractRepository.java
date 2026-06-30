package com.legalaid.contract;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {

    // All contracts for a client (not deleted)
    List<Contract> findAllByClientIdAndDeletedAtIsNull(UUID clientId);

    // All contracts for a lawyer (not deleted)
    List<Contract> findAllByLawyerIdAndDeletedAtIsNull(UUID lawyerId);

    // All contracts linked to a case
    List<Contract> findAllByCaseIdAndDeletedAtIsNull(UUID caseId);

    // Single contract — access check included
    // Client can see their own contracts
    Optional<Contract> findByIdAndClientIdAndDeletedAtIsNull(UUID id, UUID clientId);

    // Lawyer can see contracts assigned to them
    Optional<Contract> findByIdAndLawyerIdAndDeletedAtIsNull(UUID id, UUID lawyerId);

    // Any participant can view (used after role check)
    Optional<Contract> findByIdAndDeletedAtIsNull(UUID id);

    // All contracts visible to a user — client or lawyer
    @Query("""
        SELECT c FROM Contract c
        WHERE c.deletedAt IS NULL
        AND (c.clientId = :userId OR c.lawyerId = :userId)
        ORDER BY c.createdAt DESC
        """)
    List<Contract> findAllVisibleToUser(@Param("userId") UUID userId);

    // Check if a contract already exists between client and service
    // Prevents duplicate active contracts for the same service
    boolean existsByClientIdAndServiceIdAndStatusIn(
            UUID clientId, UUID serviceId, List<ContractStatus> statuses);

    long countByDeletedAtIsNull();
    long countByStatusAndDeletedAtIsNull(ContractStatus status);
}