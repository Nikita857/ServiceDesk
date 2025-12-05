package com.bm.wschat.feature.dm.service;

import com.bm.wschat.feature.dm.dto.request.SendDirectMessageRequest;
import com.bm.wschat.feature.dm.dto.response.ConversationResponse;
import com.bm.wschat.feature.dm.dto.response.DirectMessageResponse;
import com.bm.wschat.feature.dm.mapper.DirectMessageMapper;
import com.bm.wschat.feature.dm.model.DirectMessage;
import com.bm.wschat.feature.dm.repository.DirectMessageRepository;
import com.bm.wschat.feature.user.model.User;
import com.bm.wschat.feature.user.repository.UserRepository;
import com.bm.wschat.shared.dto.UserShortResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DirectMessageService {

    private final DirectMessageRepository dmRepository;
    private final UserRepository userRepository;
    private final DirectMessageMapper dmMapper;

    @Transactional
    public DirectMessageResponse sendMessage(SendDirectMessageRequest request, Long senderId) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + senderId));

        User recipient = userRepository.findById(request.recipientId())
                .orElseThrow(() -> new EntityNotFoundException("Recipient not found: " + request.recipientId()));

        if (senderId.equals(request.recipientId())) {
            throw new IllegalArgumentException("Cannot send message to yourself");
        }

        DirectMessage dm = DirectMessage.builder()
                .sender(sender)
                .recipient(recipient)
                .content(request.content())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        DirectMessage saved = dmRepository.save(dm);
        return dmMapper.toResponse(saved);
    }

    public Page<DirectMessageResponse> getConversation(Long userId, Long partnerId, Pageable pageable) {
        if (!userRepository.existsById(partnerId)) {
            throw new EntityNotFoundException("User not found: " + partnerId);
        }

        Page<DirectMessage> messages = dmRepository.findConversation(userId, partnerId, pageable);
        return messages.map(dmMapper::toResponse);
    }

    public List<ConversationResponse> getConversations(Long userId) {
        List<DirectMessage> latestMessages = dmRepository.findLatestInEachConversation(userId);

        return latestMessages.stream().map(dm -> {
            User partner = dm.getSender().getId().equals(userId) ? dm.getRecipient() : dm.getSender();
            Long unread = dmRepository.countUnreadFrom(partner.getId(), userId);

            return new ConversationResponse(
                    new UserShortResponse(partner.getId(), partner.getUsername(), partner.getFio()),
                    truncate(dm.getContent(), 100),
                    dm.getCreatedAt(),
                    unread);
        }).toList();
    }

    @Transactional
    public int markAsRead(Long senderId, Long recipientId) {
        return dmRepository.markAsReadFrom(senderId, recipientId, Instant.now());
    }

    public Long getUnreadCount(Long userId) {
        return dmRepository.countTotalUnread(userId);
    }

    @Transactional
    public void deleteMessage(Long messageId, Long userId) {
        DirectMessage dm = dmRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + messageId));

        if (!dm.getSender().getId().equals(userId)) {
            throw new AccessDeniedException("You can only delete your own messages");
        }

        dmRepository.delete(dm); // Soft delete
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
