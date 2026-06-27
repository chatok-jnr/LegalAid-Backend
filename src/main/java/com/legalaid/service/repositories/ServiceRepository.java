package com.legalaid.service.repositories;

import com.legalaid.service.LegalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceRepository extends JpaRepository<LegalService, UUID> {

    // Used by GET /api/lawyers/:id/services — public, active only
    List<LegalService> findAllByLawyerIdAndIsActiveTrueAndDeletedAtIsNull(UUID lawyerId);

    // Used by GET /api/services/mine — lawyer's own services (all statuses)
    List<LegalService> findAllByLawyerIdAndDeletedAtIsNull(UUID lawyerId);

    // Ownership check before update/delete
    Optional<LegalService> findByIdAndLawyerIdAndDeletedAtIsNull(UUID id, UUID lawyerId);

    // Active non-deleted single service — for public detail view
    Optional<LegalService> findByIdAndIsActiveTrueAndDeletedAtIsNull(UUID id);

    // Active non-deleted single service — for any authenticated user
    Optional<LegalService> findByIdAndDeletedAtIsNull(UUID id);

    // ── Browse/search — paginated ────────────────────────────
    @Query("""
        SELECT s FROM LegalService s
        WHERE s.isActive = true
        AND s.deletedAt IS NULL
        AND (:category IS NULL
             OR LOWER(s.category) LIKE LOWER(CONCAT('%', :category, '%')))
        AND (:minPrice IS NULL OR s.price >= :minPrice)
        AND (:maxPrice IS NULL OR s.price <= :maxPrice)
        AND (:deliveryDays IS NULL OR s.deliveryDays <= :deliveryDays)
        ORDER BY s.createdAt DESC
        """)
    Page<LegalService> browseServices(
            @Param("category")     String category,
            @Param("minPrice")     BigDecimal minPrice,
            @Param("maxPrice")     BigDecimal maxPrice,
            @Param("deliveryDays") Integer deliveryDays,
            Pageable pageable);
}