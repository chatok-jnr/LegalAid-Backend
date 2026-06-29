package com.legalaid.notification;

import com.legalaid.notification.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ═══════════════════════════════════════════════════════
    //  CRUD ENDPOINTS
    // ═══════════════════════════════════════════════════════

    // ── GET /api/notifications ────────────────────────────────
    // Paginated — unread first, then newest
    public NotificationPageResponse getMyNotifications(UUID userId,
                                                       int page,
                                                       int size) {
        int cappedSize = Math.min(size, 50);
        Page<Notification> notificationPage = notificationRepository
                .findAllByUserIdOrderByUnreadFirst(
                        userId, PageRequest.of(page, cappedSize));

        long unreadCount = notificationRepository
                .countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId);

        List<NotificationResponse> responses = notificationPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return NotificationPageResponse.builder()
                .notifications(responses)
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .page(page)
                .size(cappedSize)
                .unreadCount(unreadCount)
                .build();
    }

    // ── GET /api/notifications/unread-count ───────────────────
    public UnreadNotificationCount getUnreadCount(UUID userId) {
        long count = notificationRepository
                .countByUserIdAndIsReadFalseAndDeletedAtIsNull(userId);
        return UnreadNotificationCount.builder().unreadCount(count).build();
    }

    // ── PUT /api/notifications/:id/read ──────────────────────
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository
                .findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    // ── PUT /api/notifications/read-all ──────────────────────
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsRead(userId);
    }

    // ── DELETE /api/notifications/:id ────────────────────────
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        int affected = notificationRepository
                .softDeleteById(notificationId, userId);
        if (affected == 0) {
            throw new RuntimeException("Notification not found");
        }
    }

    // ═══════════════════════════════════════════════════════
    //  INTERNAL SEND METHODS — called by other services
    //  All @Async so they never block the main request thread
    // ═══════════════════════════════════════════════════════

    // ── Contract events ──────────────────────────────────────

    @Async
    public void notifyContractRequest(UUID lawyerUserId,
                                      UUID contractId,
                                      String clientName) {
        send(Notification.builder()
                .userId(lawyerUserId)
                .type(NotificationType.CONTRACT_REQUEST)
                .title("New hire request")
                .body(clientName + " wants to hire you. Review and accept or decline.")
                .actionUrl("/contracts/" + contractId)
                .referenceId(contractId)
                .referenceType("CONTRACT")
                .build());
    }

    @Async
    public void notifyContractAccepted(UUID clientUserId,
                                       UUID contractId,
                                       String lawyerName) {
        send(Notification.builder()
                .userId(clientUserId)
                .type(NotificationType.CONTRACT_ACCEPTED)
                .title("Lawyer accepted your request")
                .body(lawyerName + " accepted your hire request. Please proceed with payment.")
                .actionUrl("/contracts/" + contractId)
                .referenceId(contractId)
                .referenceType("CONTRACT")
                .build());
    }

    @Async
    public void notifyContractDeclined(UUID clientUserId,
                                       UUID contractId,
                                       String lawyerName) {
        send(Notification.builder()
                .userId(clientUserId)
                .type(NotificationType.CONTRACT_DECLINED)
                .title("Hire request declined")
                .body(lawyerName + " has declined your hire request.")
                .actionUrl("/contracts/" + contractId)
                .referenceId(contractId)
                .referenceType("CONTRACT")
                .build());
    }

    @Async
    public void notifyContractCompleted(UUID lawyerUserId,
                                        UUID contractId,
                                        String clientName) {
        send(Notification.builder()
                .userId(lawyerUserId)
                .type(NotificationType.CONTRACT_COMPLETED)
                .title("Contract marked complete")
                .body(clientName + " has marked the contract as complete. Payment will be released.")
                .actionUrl("/contracts/" + contractId)
                .referenceId(contractId)
                .referenceType("CONTRACT")
                .build());
    }

    @Async
    public void notifyContractCancelled(UUID otherUserId,
                                        UUID contractId,
                                        String cancelledByName) {
        send(Notification.builder()
                .userId(otherUserId)
                .type(NotificationType.CONTRACT_CANCELLED)
                .title("Contract cancelled")
                .body(cancelledByName + " has cancelled the contract.")
                .actionUrl("/contracts/" + contractId)
                .referenceId(contractId)
                .referenceType("CONTRACT")
                .build());
    }

    // ── Payment events ───────────────────────────────────────

    @Async
    public void notifyPaymentReceived(UUID lawyerUserId,
                                      UUID contractId,
                                      String amount) {
        send(Notification.builder()
                .userId(lawyerUserId)
                .type(NotificationType.PAYMENT_RECEIVED)
                .title("Payment received — contract is now active")
                .body("Payment of " + amount + " BDT has been verified. Your contract is now active.")
                .actionUrl("/contracts/" + contractId)
                .referenceId(contractId)
                .referenceType("CONTRACT")
                .build());
    }

    @Async
    public void notifyPaymentReleased(UUID lawyerUserId,
                                      UUID contractId,
                                      String amount) {
        send(Notification.builder()
                .userId(lawyerUserId)
                .type(NotificationType.PAYMENT_RELEASED)
                .title("Payment released to your account")
                .body(amount + " BDT has been released to your balance. You can now request a payout.")
                .actionUrl("/payments")
                .referenceId(contractId)
                .referenceType("CONTRACT")
                .build());
    }

    @Async
    public void notifyPaymentRefunded(UUID clientUserId,
                                      UUID contractId,
                                      String amount) {
        send(Notification.builder()
                .userId(clientUserId)
                .type(NotificationType.PAYMENT_REFUNDED)
                .title("Payment refunded")
                .body(amount + " BDT will be refunded to your account.")
                .actionUrl("/contracts/" + contractId)
                .referenceId(contractId)
                .referenceType("CONTRACT")
                .build());
    }

    @Async
    public void notifyPaymentRejected(UUID clientUserId,
                                      UUID contractId) {
        send(Notification.builder()
                .userId(clientUserId)
                .type(NotificationType.PAYMENT_REJECTED)
                .title("Payment verification failed")
                .body("Your transaction ID could not be verified. Please resubmit with a valid TxnID.")
                .actionUrl("/contracts/" + contractId)
                .referenceId(contractId)
                .referenceType("CONTRACT")
                .build());
    }

    // ── Dispute events ───────────────────────────────────────

    @Async
    public void notifyDisputeRaised(UUID otherUserId,
                                    UUID disputeId,
                                    UUID contractId) {
        send(Notification.builder()
                .userId(otherUserId)
                .type(NotificationType.DISPUTE_RAISED)
                .title("A dispute has been raised")
                .body("A dispute has been raised on your contract. Our team will review it shortly.")
                .actionUrl("/disputes/" + disputeId)
                .referenceId(disputeId)
                .referenceType("DISPUTE")
                .build());
    }

    @Async
    public void notifyDisputeResolved(UUID userId,
                                      UUID disputeId,
                                      String resolution) {
        send(Notification.builder()
                .userId(userId)
                .type(NotificationType.DISPUTE_RESOLVED)
                .title("Dispute resolved")
                .body("Your dispute has been resolved: " + resolution)
                .actionUrl("/disputes/" + disputeId)
                .referenceId(disputeId)
                .referenceType("DISPUTE")
                .build());
    }

    // ── Review events ────────────────────────────────────────

    @Async
    public void notifyReviewReceived(UUID lawyerUserId,
                                     UUID reviewId,
                                     String clientName,
                                     int rating) {
        send(Notification.builder()
                .userId(lawyerUserId)
                .type(NotificationType.REVIEW_RECEIVED)
                .title("You received a new review")
                .body(clientName + " left you a " + rating + "-star review.")
                .actionUrl("/reviews")
                .referenceId(reviewId)
                .referenceType("REVIEW")
                .build());
    }

    @Async
    public void notifyReviewReplied(UUID clientUserId,
                                    UUID reviewId,
                                    String lawyerName) {
        send(Notification.builder()
                .userId(clientUserId)
                .type(NotificationType.REVIEW_REPLIED)
                .title("Lawyer replied to your review")
                .body(lawyerName + " has replied to your review.")
                .actionUrl("/reviews")
                .referenceId(reviewId)
                .referenceType("REVIEW")
                .build());
    }

    // ── Verification events ──────────────────────────────────

    @Async
    public void notifyVerificationApproved(UUID lawyerUserId) {
        send(Notification.builder()
                .userId(lawyerUserId)
                .type(NotificationType.VERIFICATION_APPROVED)
                .title("Your account has been verified")
                .body("Congratulations! Your lawyer account is now verified. You can start creating services.")
                .actionUrl("/profile")
                .build());
    }

    @Async
    public void notifyVerificationRejected(UUID lawyerUserId,
                                           String reason) {
        send(Notification.builder()
                .userId(lawyerUserId)
                .type(NotificationType.VERIFICATION_REJECTED)
                .title("Verification request rejected")
                .body("Your verification documents were rejected. Reason: " + reason
                        + ". Please resubmit.")
                .actionUrl("/onboarding")
                .build());
    }

    // ── Message event ────────────────────────────────────────

    @Async
    public void notifyMessageReceived(UUID recipientUserId,
                                      UUID contractId,
                                      String senderName) {
        send(Notification.builder()
                .userId(recipientUserId)
                .type(NotificationType.MESSAGE_RECEIVED)
                .title("New message from " + senderName)
                .body(senderName + " sent you a message.")
                .actionUrl("/messages/" + contractId)
                .referenceId(contractId)
                .referenceType("CONTRACT")
                .build());
    }

    // ── Case invite ──────────────────────────────────────────

    @Async
    public void notifyCaseInvite(UUID invitedUserId,
                                 UUID caseId,
                                 String inviterName,
                                 String caseTitle) {
        send(Notification.builder()
                .userId(invitedUserId)
                .type(NotificationType.CASE_INVITE)
                .title("You were invited to a case")
                .body(inviterName + " invited you to collaborate on: " + caseTitle)
                .actionUrl("/cases/" + caseId)
                .referenceId(caseId)
                .referenceType("CASE")
                .build());
    }

    // ── Private helper ───────────────────────────────────────
    private void send(Notification notification) {
        notificationRepository.save(notification);
    }

    // ── Mapper ───────────────────────────────────────────────
    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType().name())
                .title(n.getTitle())
                .body(n.getBody())
                .isRead(n.getIsRead())
                .actionUrl(n.getActionUrl())
                .referenceId(n.getReferenceId())
                .referenceType(n.getReferenceType())
                .createdAt(n.getCreatedAt())
                .build();
    }
}