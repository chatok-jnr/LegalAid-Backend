package com.legalaid.case_.repositories;

import com.legalaid.case_.entitys.LegalCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRepository extends JpaRepository<LegalCase, UUID> {

    // All cases owned by a client (not deleted)
    List<LegalCase> findAllByClientIdAndDeletedAtIsNull(UUID clientId);

    // All cases assigned to a lawyer (not deleted)
    List<LegalCase> findAllByLawyerIdAndDeletedAtIsNull(UUID lawyerId);

    // Active case by id — ownership check for client
    Optional<LegalCase> findByIdAndClientIdAndDeletedAtIsNull(UUID id, UUID clientId);

    // Active case by id — any access (used after access check)
    Optional<LegalCase> findByIdAndDeletedAtIsNull(UUID id);

    // Cases a user has been given access to via case_access table
    // Used when a lawyer or other user is invited to view a case
    @Query("""
        SELECT c FROM LegalCase c
        JOIN CaseAccess ca ON ca.legalCase.id = c.id
        WHERE ca.userId = :userId
        AND c.deletedAt IS NULL
        """)
    List<LegalCase> findAllAccessibleByUserId(@Param("userId") UUID userId);

    // All cases a user can see — owned or invited
    @Query("""
        SELECT DISTINCT c FROM LegalCase c
        LEFT JOIN CaseAccess ca ON ca.legalCase.id = c.id
        WHERE c.deletedAt IS NULL
        AND (c.clientId = :userId
             OR c.lawyerId = :userId
             OR ca.userId = :userId)
        """)
    List<LegalCase> findAllVisibleToUser(@Param("userId") UUID userId);
}