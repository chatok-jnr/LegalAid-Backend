package com.legalaid.message;

import com.legalaid.common.response.ApiResponse;
import com.legalaid.message.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // ── GET /api/contracts/:id/messages ──────────────────────
    // Paginated message thread — newest messages first.
    // Also marks all unread messages from other party as read.
    // Query params: page (default 0), size (default 20, max 50)
    @GetMapping("/api/contracts/{contractId}/messages")
    public ResponseEntity<ApiResponse<MessageThreadResponse>> getMessages(
            @PathVariable UUID contractId,
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        ApiResponse<MessageThreadResponse> body = ApiResponse.success(
                messageService.getMessages(contractId, userId, page, size)
        );
        return ResponseEntity.ok(body);
    }

    // ── POST /api/contracts/:id/messages ─────────────────────
    // Send a message in a contract thread.
    // Both client and lawyer can send.
    // Blocked on CANCELLED contracts.
    @PostMapping("/api/contracts/{contractId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> sendMessage(
            @PathVariable UUID contractId,
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody SendMessageRequest request) {

        ApiResponse<MessageResponse> body = ApiResponse.success(
                messageService.sendMessage(contractId, userId, request)
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    // ── GET /api/messages/unread-count ────────────────────────
    // Total unread messages across all contracts for this user.
    // Frontend polls this every 30 seconds for the nav badge.
    @GetMapping("/api/messages/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal UUID userId) {

        ApiResponse<UnreadCountResponse> body = ApiResponse.success(
                messageService.getUnreadCount(userId)
        );
        return ResponseEntity.ok(body);
    }
}