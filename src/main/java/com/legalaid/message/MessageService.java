package com.legalaid.message;

import com.legalaid.contract.Contract;
import com.legalaid.contract.ContractRepository;
import com.legalaid.message.dto.*;
import com.legalaid.user.User;
import com.legalaid.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository  messageRepository;
    private final ContractRepository contractRepository;
    private final UserRepository     userRepository;

    // ── GET /api/contracts/:id/messages ──────────────────────
    // Returns paginated message thread for a contract.
    // Also marks all messages from the other party as read
    // when the user opens the thread.
    @Transactional
    public MessageThreadResponse getMessages(UUID contractId,
                                             UUID requesterId,
                                             int page,
                                             int size) {
        // Verify access — only contract participants can read messages
        Contract contract = findContractWithAccess(contractId, requesterId);

        // Cap page size to prevent abuse
        int cappedSize = Math.min(size, 50);
        Pageable pageable = PageRequest.of(page, cappedSize);

        Page<Message> messagePage = messageRepository
                .findAllByContractIdOrderByCreatedAtDesc(contractId, pageable);

        // Mark messages from the other party as read
        // Returns count of messages marked — useful for future notification updates
        messageRepository.markAllAsRead(contractId, requesterId);

        long unreadCount = messageRepository
                .countByContractIdAndSenderIdNotAndIsReadFalse(
                        contractId, requesterId);

        List<MessageResponse> responses = messagePage.getContent()
                .stream()
                .map(this::toMessageResponse)
                .toList();

        return MessageThreadResponse.builder()
                .contractId(contractId)
                .totalMessages(messagePage.getTotalElements())
                .unreadCount(unreadCount)
                .messages(responses)
                .page(page)
                .size(cappedSize)
                .totalPages(messagePage.getTotalPages())
                .build();
    }

    // ── POST /api/contracts/:id/messages ─────────────────────
    // Send a message in a contract thread.
    // Both client and lawyer can send messages.
    // Only allowed on non-cancelled, non-completed contracts
    // (messaging on completed contracts still allowed for
    //  post-completion questions — only cancelled is blocked)
    @Transactional
    public MessageResponse sendMessage(UUID contractId,
                                       UUID senderId,
                                       SendMessageRequest request) {
        // Verify access
        Contract contract = findContractWithAccess(contractId, senderId);

        // Block messaging on cancelled contracts only
        if (contract.getStatus().name().equals("CANCELLED")) {
            throw new RuntimeException(
                    "Cannot send messages on a cancelled contract");
        }

        Message message = Message.builder()
                .contractId(contractId)
                .senderId(senderId)
                .text(request.getText())
                .attachmentUrl(request.getAttachmentUrl())
                .attachmentName(request.getAttachmentName())
                .isRead(false)
                .build();

        message = messageRepository.save(message);
        return toMessageResponse(message);
    }

    // ── GET /api/messages/unread-count ────────────────────────
    // Returns total unread count across all contracts.
    // Frontend uses this to show the nav badge.
    public UnreadCountResponse getUnreadCount(UUID userId) {
        long count = messageRepository.countAllUnreadForUser(userId);
        return UnreadCountResponse.builder()
                .totalUnread(count)
                .build();
    }

    // ── Access control ───────────────────────────────────────
    // Only the client and lawyer of the contract can message
    private Contract findContractWithAccess(UUID contractId, UUID requesterId) {
        Contract contract = contractRepository
                .findByIdAndDeletedAtIsNull(contractId)
                .orElseThrow(() -> new RuntimeException("Contract not found"));

        boolean isParticipant = contract.getClientId().equals(requesterId)
                || contract.getLawyerId().equals(requesterId);

        if (!isParticipant) {
            throw new RuntimeException(
                    "You do not have access to this conversation");
        }
        return contract;
    }

    // ── Mapper ───────────────────────────────────────────────
    private MessageResponse toMessageResponse(Message message) {
        // Fetch sender info for display
        User sender = userRepository
                .findActiveById(message.getSenderId())
                .orElse(null);

        return MessageResponse.builder()
                .id(message.getId())
                .contractId(message.getContractId())
                .senderId(message.getSenderId())
                .senderName(sender != null ? sender.getName() : null)
                .senderAvatarUrl(sender != null ? sender.getAvatarUrl() : null)
                .text(message.getText())
                .attachmentUrl(message.getAttachmentUrl())
                .attachmentName(message.getAttachmentName())
                .isRead(message.getIsRead())
                .createdAt(message.getCreatedAt())
                .build();
    }
}