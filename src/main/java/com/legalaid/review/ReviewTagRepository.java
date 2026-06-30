package com.legalaid.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReviewTagRepository extends JpaRepository<ReviewTag, UUID> {

    List<ReviewTag> findAllByReview_Id(UUID reviewId);
    void deleteAllByReview_Id(UUID reviewId);
}