package com.mpc.controller;

import com.mpc.dto.GroupDto;
import com.mpc.model.User;
import com.mpc.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupDto.GroupInfo> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody GroupDto.CreateRequest req) {
        return ResponseEntity.ok(groupService.toInfo(groupService.createGroup(user.getId(), req)));
    }

    @DeleteMapping("/{groupId}")
    public ResponseEntity<String> dissolve(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId) {
        groupService.dissolveGroup(user.getId(), groupId);
        return ResponseEntity.ok("群组已解散");
    }

    @PutMapping("/{groupId}")
    public ResponseEntity<GroupDto.GroupInfo> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @Valid @RequestBody GroupDto.UpdateRequest req) {
        return ResponseEntity.ok(groupService.toInfo(groupService.updateGroup(user.getId(), groupId, req)));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<GroupDto.GroupInfo>> myGroups(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(groupService.getMyGroups(user.getId()));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<GroupDto.GroupInfo> getGroup(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getGroupInfo(groupId));
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupDto.MemberInfo>> getMembers(@PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getMembers(groupId));
    }

    @PostMapping("/{groupId}/join")
    public ResponseEntity<String> requestJoin(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId) {
        groupService.requestJoin(user.getId(), groupId);
        return ResponseEntity.ok("申请已发送");
    }

    @PostMapping("/{groupId}/leave")
    public ResponseEntity<String> leave(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId) {
        groupService.leaveGroup(user.getId(), groupId);
        return ResponseEntity.ok("已退出群组");
    }

    @GetMapping("/{groupId}/requests")
    public ResponseEntity<List<GroupDto.JoinRequestInfo>> getPendingRequests(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId) {
        return ResponseEntity.ok(groupService.getPendingRequests(user.getId(), groupId));
    }

    @PostMapping("/requests/{requestId}/handle")
    public ResponseEntity<String> handleRequest(
            @AuthenticationPrincipal User user,
            @PathVariable Long requestId,
            @RequestBody Map<String, Boolean> body) {
        boolean approve = Boolean.TRUE.equals(body.get("approve"));
        groupService.handleJoinRequest(user.getId(), requestId, approve);
        return ResponseEntity.ok(approve ? "已通过" : "已拒绝");
    }

    @PostMapping("/{groupId}/kick")
    public ResponseEntity<String> kick(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @RequestBody GroupDto.KickRequest req) {
        groupService.kickMember(user.getId(), groupId, req.getUserId());
        return ResponseEntity.ok("已踢出");
    }

    @PostMapping("/{groupId}/admin")
    public ResponseEntity<String> setAdmin(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @RequestBody GroupDto.AdminRequest req,
            @RequestParam(defaultValue = "true") boolean grant) {
        groupService.setAdmin(user.getId(), groupId, req.getUserId(), grant);
        return ResponseEntity.ok(grant ? "已设为管理员" : "已撤销管理员");
    }
}
