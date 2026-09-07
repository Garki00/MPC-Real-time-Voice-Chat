package com.mpc.controller;

import com.mpc.dto.UserDto;
import com.mpc.model.User;
import com.mpc.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public ResponseEntity<List<UserDto.UserInfo>> getFriends(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(friendService.getFriends(user.getId()));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<UserDto.UserInfo>> getPendingRequests(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(friendService.getPendingRequests(user.getId()));
    }

    @PostMapping("/request/{targetId}")
    public ResponseEntity<String> sendRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long targetId) {
        friendService.sendRequest(user.getId(), targetId);
        return ResponseEntity.ok("好友申请已发送");
    }

    @PostMapping("/request/{requesterId}/handle")
    public ResponseEntity<String> handleRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long requesterId,
            @RequestBody Map<String, Boolean> body) {
        boolean accept = Boolean.TRUE.equals(body.get("accept"));
        friendService.handleRequest(user.getId(), requesterId, accept);
        return ResponseEntity.ok(accept ? "已接受" : "已拒绝");
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<String> deleteFriend(
            @AuthenticationPrincipal User user,
            @PathVariable Long friendId) {
        friendService.deleteFriend(user.getId(), friendId);
        return ResponseEntity.ok("已删除好友");
    }
}
