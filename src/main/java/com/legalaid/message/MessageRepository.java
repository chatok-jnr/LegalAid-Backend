package com.legalaid.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // Paginated message thread for a contract — newest first
    Page<Message> findAllByContractIdOrderByCreatedAtDesc(
            UUID contractId, Pageable pageable);

    // Count unread messages in a contract for a specific user
    // Used to show unread badge on the messages page
    long countByContractIdAndSenderIdNotAndIsReadFalse(
            UUID contractId, UUID userId);

    // Mark all messages in a contract as read for the other party
    // Called when a user opens the message thread
    @Modifying
    @Query("""
        UPDATE Message m
        SET m.isRead = true
        WHERE m.contractId = :contractId
        AND m.senderId != :readerId
        AND m.isRead = false
        """)
    int markAllAsRead(
            @Param("contractId") UUID contractId,
            @Param("readerId")   UUID readerId);

    // Total unread messages across ALL contracts for a user
    // Used for the notification badge on the messages nav icon
    @Query("""
        SELECT COUNT(m) FROM Message m
        JOIN Contract c ON c.id = m.contractId
        WHERE m.isRead = false
        AND m.senderId != :userId
        AND (c.clientId = :userId OR c.lawyerId = :userId)
        AND c.deletedAt IS NULL
        """)
    long countAllUnreadForUser(@Param("userId") UUID userId);
}