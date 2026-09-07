package com.mpc.controller;

import com.mpc.dto.ChatDto;
import com.mpc.model.User;
import com.mpc.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    // STOMP: client sends to /app/chat.private
    @MessageMapping("/chat.private")
    public void handlePrivate(Principal principal, @Payload ChatDto.PrivateMessage msg) {
        Long senderId = Long.parseLong(principal.getName());
        chatService.sendPrivate(senderId, msg);
    }

    // STOMP: client sends to /app/chat.group
    @MessageMapping("/chat.group")
    public void handleGroup(Principal principal, @Payload ChatDto.GroupMessage msg) {
        Long senderId = Long.parseLong(principal.getName());
        chatService.sendGroup(senderId, msg);
    }

    // REST: fetch history
    @GetMapping("/api/chat/private/{friendId}")
    public ResponseEntity<List<ChatDto.MessagePayload>> privateHistory(
            @AuthenticationPrincipal User user,
            @PathVariable Long friendId) {
        return ResponseEntity.ok(chatService.getPrivateHistory(user.getId(), friendId));
    }

    @GetMapping("/api/chat/group/{groupId}")
    public ResponseEntity<List<ChatDto.MessagePayload>> groupHistory(
            @PathVariable Long groupId) {
        return ResponseEntity.ok(chatService.getGroupHistory(groupId));
    }
}
