package com.mpc.controller;

import com.mpc.model.MessageReadStatus;
import com.mpc.model.User;
import com.mpc.service.MessageReadStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageReadStatusController {

    private final MessageReadStatusService readStatusService;

    @PostMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal User user,
            @RequestParam String conversationType,
            @RequestParam Long conversationId,
            @RequestParam Long messageId) {

        MessageReadStatus.ConversationType type = MessageReadStatus.ConversationType.valueOf(conversationType.toUpperCase());
        readStatusService.markAsRead(user.getId(), type, conversationId, messageId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-counts")
    public ResponseEntity<Map<String, Integer>> getUnreadCounts(@AuthenticationPrincipal User user) {
        Map<String, Integer> counts = readStatusService.getUnreadCounts(user.getId());
        return ResponseEntity.ok(counts);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Integer> getUnreadCount(
            @AuthenticationPrincipal User user,
            @RequestParam String conversationType,
            @RequestParam Long conversationId) {

        MessageReadStatus.ConversationType type = MessageReadStatus.ConversationType.valueOf(conversationType.toUpperCase());
        int count = readStatusService.getUnreadCount(user.getId(), type, conversationId);
        return ResponseEntity.ok(count);
    }
}
