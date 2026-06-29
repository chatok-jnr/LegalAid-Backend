package com.legalaid.lawyer.repositories;

import com.legalaid.lawyer.LawyerProfile;
import com.legalaid.lawyer.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LawyerRepository extends JpaRepository<LawyerProfile, UUID> {
    boolean existsByUserId(UUID userId);

    Optional<LawyerProfile> findByUserId(UUID userId);

    Page<LawyerProfile> findAllByVerificationStatus(VerificationStatus verificationStatus, Pageable pageable);

    @Query("""
        SELECT DISTINCT lp from LawyerProfile lp
        LEFT JOIN LawyerPracticeArea la ON la.lawyer.id = lp.id
        LEFT JOIN LawyerLocation ll ON ll.lawyer.id = lp.id
        WHERE lp.verificationStatus = 'APPROVED' 
        AND (:category IS NULL OR LOWER(la.area) LIKE LOWER(CONCAT('%', :category, '%')))
        AND(:city IS NULL OR LOWER(ll.city) LIKE LOWER(CONCAT('%', :city, '%')))
        AND(:minRating IS NULL OR lp.rating >= :minRating)
        ORDER BY lp.rating DESC
        """)
    Page<LawyerProfile> searchLawyers(
            @Param("category") String category,
            @Param("city") String city,
            @Param("minRating") Double minRating,
            Pageable pageable
    );

    @Query("""
    SELECT l.userId FROM LawyerProfile l
    WHERE l.id = :lawyerProfileId
    AND l.verificationStatus = 'APPROVED'
    """)
    Optional<UUID> getLawyerUserIdByLawyerProfileId(@Param("lawyerProfileId") UUID lawyerProfileId);
}
