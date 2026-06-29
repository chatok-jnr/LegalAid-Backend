package com.legalaid.notification;

import com.legalaid.common.response.ApiResponse;
import com.legalaid.notification.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // ── GET /api/notifications ────────────────────────────────
    // Paginated — unread first, then newest
    // Query params: page (default 0), size (default 20, max 50)
    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPageResponse>> getMyNotifications(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        ApiResponse<NotificationPageResponse> body = ApiResponse.success(
                notificationService.getMyNotifications(userId, page, size)
        );
        return ResponseEntity.ok(body);
    }

    // ── GET /api/notifications/unread-count ───────────────────
    // Lightweight endpoint — frontend polls every 30 seconds
    // for the nav badge count
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadNotificationCount>> getUnreadCount(
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<UnreadNotificationCount> body = ApiResponse.success(
                notificationService.getUnreadCount(userId)
        );
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/notifications/:id/read ──────────────────────
    // Mark a single notification as read
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        notificationService.markAsRead(id, userId);
        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }

    // ── PUT /api/notifications/read-all ──────────────────────
    // Mark all as read in one shot
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UUID userId) {

        notificationService.markAllAsRead(userId);
        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }

    // ── DELETE /api/notifications/:id ────────────────────────
    // Soft delete — user dismisses a notification
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable UUID id,
            @AuthenticationPrincipal UUID userId) {

        notificationService.deleteNotification(id, userId);
        ApiResponse<Void> body = ApiResponse.success(null);
        return ResponseEntity.ok(body);
    }
}