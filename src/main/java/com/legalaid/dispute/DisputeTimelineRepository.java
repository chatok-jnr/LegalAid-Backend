package com.legalaid.dispute;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DisputeTimelineRepository extends JpaRepository<DisputeTimeline, UUID> {

    List<DisputeTimeline> findAllByDispute_IdOrderByCreatedAtAsc(UUID disputeId);
}