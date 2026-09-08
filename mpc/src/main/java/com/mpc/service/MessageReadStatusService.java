package com.mpc.service;

import com.mpc.model.Message;
import com.mpc.model.MessageReadStatus;
import com.mpc.repository.MessageReadStatusRepository;
import com.mpc.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageReadStatusService {

    private final MessageReadStatusRepository readStatusRepository;
    private final MessageRepository messageRepository;

    @Transactional
    public void markAsRead(Long userId, MessageReadStatus.ConversationType conversationType, Long conversationId, Long messageId) {
        MessageReadStatus status = readStatusRepository
                .findByUserIdAndConversationTypeAndConversationId(userId, conversationType, conversationId)
                .orElse(new MessageReadStatus());

        status.setUserId(userId);
        status.setConversationType(conversationType);
        status.setConversationId(conversationId);
        status.setLastReadMessageId(messageId);
        status.setLastReadAt(LocalDateTime.now());

        readStatusRepository.save(status);
    }

    public Map<String, Integer> getUnreadCounts(Long userId) {
        Map<String, Integer> unreadCounts = new HashMap<>();

        List<MessageReadStatus> statuses = readStatusRepository.findByUserId(userId);

        for (MessageReadStatus status : statuses) {
            int unreadCount = calculateUnreadCount(status);
            String key = status.getConversationType().name().toLowerCase() + "_" + status.getConversationId();
            unreadCounts.put(key, unreadCount);
        }

        return unreadCounts;
    }

    public int getUnreadCount(Long userId, MessageReadStatus.ConversationType conversationType, Long conversationId) {
        MessageReadStatus status = readStatusRepository
                .findByUserIdAndConversationTypeAndConversationId(userId, conversationType, conversationId)
                .orElse(null);

        if (status == null) {
            return getTotalMessageCount(conversationType, conversationId, userId);
        }

        return calculateUnreadCount(status);
    }

    private int calculateUnreadCount(MessageReadStatus status) {
        Long lastReadMessageId = status.getLastReadMessageId();

        if (lastReadMessageId == null) {
            return getTotalMessageCount(status.getConversationType(), status.getConversationId(), status.getUserId());
        }

        List<Message> messages;
        if (status.getConversationType() == MessageReadStatus.ConversationType.PRIVATE) {
            messages = messageRepository.findPrivateHistory(status.getUserId(), status.getConversationId());
        } else {
            messages = messageRepository.findByGroupIdOrderByCreatedAtAsc(status.getConversationId());
        }

        int unreadCount = 0;
        boolean foundLastRead = false;
        for (Message msg : messages) {
            if (msg.getId().equals(lastReadMessageId)) {
                foundLastRead = true;
                continue;
            }
            if (foundLastRead && !msg.getSenderId().equals(status.getUserId())) {
                unreadCount++;
            }
        }

        return unreadCount;
    }

    private int getTotalMessageCount(MessageReadStatus.ConversationType conversationType, Long conversationId, Long userId) {
        List<Message> messages;
        if (conversationType == MessageReadStatus.ConversationType.PRIVATE) {
            messages = messageRepository.findPrivateHistory(userId, conversationId);
        } else {
            messages = messageRepository.findByGroupIdOrderByCreatedAtAsc(conversationId);
        }

        return (int) messages.stream()
                .filter(msg -> !msg.getSenderId().equals(userId))
                .count();
    }
}
