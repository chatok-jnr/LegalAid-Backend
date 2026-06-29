package com.legalaid.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Paginated notifications for a user — newest first, unread first
    @Query("""
        SELECT n FROM Notification n
        WHERE n.userId = :userId
        AND n.deletedAt IS NULL
        ORDER BY n.isRead ASC, n.createdAt DESC
        """)
    Page<Notification> findAllByUserIdOrderByUnreadFirst(
            @Param("userId") UUID userId, Pageable pageable);

    // Count unread — for nav badge
    long countByUserIdAndIsReadFalseAndDeletedAtIsNull(UUID userId);

    // Find single notification — access check included
    Optional<Notification> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    // Mark all as read in one query
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.isRead = true
        WHERE n.userId = :userId
        AND n.isRead = false
        AND n.deletedAt IS NULL
        """)
    int markAllAsRead(@Param("userId") UUID userId);

    // Soft delete single notification
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.deletedAt = CURRENT_TIMESTAMP
        WHERE n.id = :id
        AND n.userId = :userId
        AND n.deletedAt IS NULL
        """)
    int softDeleteById(@Param("id") UUID id, @Param("userId") UUID userId);
}