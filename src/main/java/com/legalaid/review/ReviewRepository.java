package com.legalaid.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    // Public review listing for a lawyer — paginated
    Page<Review> findAllByLawyerIdAndDeletedAtIsNull(
            UUID lawyerId, Pageable pageable);

    // Check if client already reviewed this contract
    boolean existsByContractIdAndDeletedAtIsNull(UUID contractId);

    // Find review by contract — for reply and helpful vote
    Optional<Review> findByContractIdAndDeletedAtIsNull(UUID contractId);

    // Find by id — ownership check for client
    Optional<Review> findByIdAndClientIdAndDeletedAtIsNull(UUID id, UUID clientId);

    // Find by id — ownership check for lawyer reply
    Optional<Review> findByIdAndLawyerIdAndDeletedAtIsNull(UUID id, UUID lawyerId);

    // Average rating for a lawyer — used as fallback if DB trigger not firing
    @Query("""
        SELECT COALESCE(AVG(r.rating), 0)
        FROM Review r
        WHERE r.lawyerId = :lawyerId
        AND r.deletedAt IS NULL
        """)
    Double averageRatingByLawyerId(@Param("lawyerId") UUID lawyerId);

    // Count reviews for a lawyer
    long countByLawyerIdAndDeletedAtIsNull(UUID lawyerId);
}