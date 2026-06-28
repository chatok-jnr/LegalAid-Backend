package com.legalaid.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // All documents owned by a user (not deleted)
    List<Document> findAllByOwnerIdAndDeletedAtIsNull(UUID ownerId);

    // Filtered by folder
    List<Document> findAllByOwnerIdAndFolderNameAndDeletedAtIsNull(
            UUID ownerId, String folderName);

    // Starred documents
    List<Document> findAllByOwnerIdAndIsStarredTrueAndDeletedAtIsNull(UUID ownerId);

    // Documents linked to a case
    List<Document> findAllByCaseIdAndDeletedAtIsNull(UUID caseId);

    // Documents linked to a contract
    List<Document> findAllByContractIdAndDeletedAtIsNull(UUID contractId);

    // Single document — ownership check
    Optional<Document> findByIdAndOwnerIdAndDeletedAtIsNull(UUID id, UUID ownerId);

    // Single document — any access (for shared docs or case/contract participants)
    Optional<Document> findByIdAndDeletedAtIsNull(UUID id);

    // All distinct folder names for a user — for folder sidebar
    @Query("""
        SELECT DISTINCT d.folderName FROM Document d
        WHERE d.ownerId = :ownerId
        AND d.folderName IS NOT NULL
        AND d.deletedAt IS NULL
        ORDER BY d.folderName ASC
        """)
    List<String> findDistinctFoldersByOwnerId(@Param("ownerId") UUID ownerId);
}