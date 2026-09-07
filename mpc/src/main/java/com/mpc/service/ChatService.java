package com.mpc.service;

import com.mpc.dto.ChatDto;
import com.mpc.model.Message;
import com.mpc.repository.MessageRepository;
import com.mpc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatDto.MessagePayload sendPrivate(Long senderId, ChatDto.PrivateMessage req) {
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setReceiverId(req.getReceiverId());
        msg.setContent(req.getContent());
        msg.setType(req.getType());
        messageRepository.save(msg);

        ChatDto.MessagePayload payload = toPayload(msg);
        messagingTemplate.convertAndSendToUser(
                String.valueOf(req.getReceiverId()), "/queue/messages", payload);
        return payload;
    }

    @Transactional
    public ChatDto.MessagePayload sendGroup(Long senderId, ChatDto.GroupMessage req) {
        Message msg = new Message();
        msg.setSenderId(senderId);
        msg.setGroupId(req.getGroupId());
        msg.setContent(req.getContent());
        msg.setType(req.getType());
        messageRepository.save(msg);

        ChatDto.MessagePayload payload = toPayload(msg);
        messagingTemplate.convertAndSend("/topic/group/" + req.getGroupId(), payload);
        return payload;
    }

    @Transactional(readOnly = true)
    public List<ChatDto.MessagePayload> getPrivateHistory(Long userId, Long friendId) {
        return messageRepository.findPrivateHistory(userId, friendId).stream()
                .map(this::toPayload)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatDto.MessagePayload> getGroupHistory(Long groupId) {
        return messageRepository.findByGroupIdOrderByCreatedAtAsc(groupId).stream()
                .map(this::toPayload)
                .collect(Collectors.toList());
    }

    private ChatDto.MessagePayload toPayload(Message msg) {
        ChatDto.MessagePayload p = new ChatDto.MessagePayload();
        p.setId(msg.getId());
        p.setSenderId(msg.getSenderId());
        p.setReceiverId(msg.getReceiverId());
        p.setGroupId(msg.getGroupId());
        p.setContent(msg.getContent());
        p.setType(msg.getType());
        p.setCreatedAt(msg.getCreatedAt().toString());
        userRepository.findById(msg.getSenderId()).ifPresent(u -> {
            p.setSenderName(u.getUsername());
            p.setSenderAvatar(u.getAvatar());
        });
        return p;
    }
}
