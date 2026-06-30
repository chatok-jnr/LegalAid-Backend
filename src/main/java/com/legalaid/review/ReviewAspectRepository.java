package com.legalaid.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewAspectRepository extends JpaRepository<ReviewAspect, UUID> {

    List<ReviewAspect> findAllByReview_Id(UUID reviewId);
    void deleteAllByReview_Id(UUID reviewId);
}