package com.mpc.service;

import com.mpc.dto.GroupDto;
import com.mpc.dto.NotificationDto;
import com.mpc.model.*;
import com.mpc.model.GroupMember.MemberRole;
import com.mpc.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupJoinRequestRepository joinRequestRepository;
    private final GroupAnnouncementRepository announcementRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final OnlineStatusService onlineStatusService;

    @Transactional
    public Group createGroup(Long ownerId, GroupDto.CreateRequest req) {
        Group group = new Group();
        group.setName(req.getName());
        group.setAvatar(req.getAvatar());
        group.setOwnerId(ownerId);
        group = groupRepository.save(group);

        GroupMember ownerMember = new GroupMember();
        ownerMember.setGroup(group);
        ownerMember.setUserId(ownerId);
        ownerMember.setRole(MemberRole.OWNER);
        groupMemberRepository.save(ownerMember);
        return group;
    }

    @Transactional
    public void dissolveGroup(Long ownerId, Long groupId) {
        Group group = getGroupOrThrow(groupId);
        if (!group.getOwnerId().equals(ownerId)) throw new IllegalArgumentException("只有群主可以解散群组");
        // notify all members before deleting
        String groupName = group.getName();
        groupMemberRepository.findByGroupId(groupId).forEach(m -> {
            if (!m.getUserId().equals(ownerId)) {
                messagingTemplate.convertAndSendToUser(
                    String.valueOf(m.getUserId()), "/queue/notifications",
                    new NotificationDto("GROUP_DISSOLVED", Map.of("groupId", groupId, "groupName", groupName)));
            }
        });
        groupRepository.delete(group);
    }

    @Transactional
    public Group updateGroup(Long userId, Long groupId, GroupDto.UpdateRequest req) {
        Group group = getGroupOrThrow(groupId);
        assertOwnerOrAdmin(userId, groupId);
        if (req.getName() != null) group.setName(req.getName());
        if (req.getAvatar() != null) group.setAvatar(req.getAvatar());
        if (req.getAnnouncement() != null) {
            group.setAnnouncement(req.getAnnouncement());
            // notify all members
            String announcement = req.getAnnouncement();
            groupMemberRepository.findByGroupId(groupId).forEach(m ->
                messagingTemplate.convertAndSendToUser(
                    String.valueOf(m.getUserId()), "/queue/notifications",
                    new NotificationDto("ANNOUNCEMENT", Map.of("groupId", groupId, "content", announcement))));
        }
        return groupRepository.save(group);
    }

    @Transactional
    public void requestJoin(Long userId, Long groupId) {
        Group group = getGroupOrThrow(groupId);
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId))
            throw new IllegalArgumentException("已在群组中");
        joinRequestRepository.findByGroupIdAndUserId(groupId, userId).ifPresent(r -> {
            if (r.getStatus() == GroupJoinRequest.RequestStatus.PENDING)
                throw new IllegalArgumentException("已发送过申请");
        });
        GroupJoinRequest req = new GroupJoinRequest();
        req.setGroupId(groupId);
        req.setUserId(userId);
        joinRequestRepository.save(req);

        // notify owner and admins
        List<GroupMember> managers = groupMemberRepository.findByGroupId(groupId).stream()
                .filter(m -> m.getRole() == MemberRole.OWNER || m.getRole() == MemberRole.ADMIN)
                .collect(Collectors.toList());
        User applicant = userRepository.findById(userId).orElseThrow();
        managers.forEach(m ->
            messagingTemplate.convertAndSendToUser(
                String.valueOf(m.getUserId()), "/queue/notifications",
                new NotificationDto("GROUP_JOIN_REQUEST",
                    Map.of("groupId", groupId, "groupName", group.getName(), "userId", userId, "username", applicant.getUsername()))));
    }

    @Transactional
    public void handleJoinRequest(Long managerId, Long requestId, boolean approve) {
        GroupJoinRequest req = joinRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("申请不存在"));
        assertOwnerOrAdmin(managerId, req.getGroupId());
        if (req.getStatus() != GroupJoinRequest.RequestStatus.PENDING)
            throw new IllegalArgumentException("申请已处理");

        req.setStatus(approve ? GroupJoinRequest.RequestStatus.APPROVED : GroupJoinRequest.RequestStatus.REJECTED);
        joinRequestRepository.save(req);

        if (approve) {
            GroupMember member = new GroupMember();
            member.setGroup(groupRepository.getReferenceById(req.getGroupId()));
            member.setUserId(req.getUserId());
            member.setRole(MemberRole.MEMBER);
            groupMemberRepository.save(member);
        }

        String notifType = approve ? "GROUP_JOIN_APPROVED" : "GROUP_JOIN_REJECTED";
        Group group = getGroupOrThrow(req.getGroupId());
        messagingTemplate.convertAndSendToUser(
            String.valueOf(req.getUserId()), "/queue/notifications",
            new NotificationDto(notifType, Map.of("groupId", req.getGroupId(), "groupName", group.getName())));
    }

    @Transactional
    public void leaveGroup(Long userId, Long groupId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("不在群组中"));
        if (member.getRole() == MemberRole.OWNER) throw new IllegalArgumentException("群主请先转让或解散群组");
        groupMemberRepository.delete(member);
    }

    @Transactional
    public void kickMember(Long managerId, Long groupId, Long targetId) {
        assertOwnerOrAdmin(managerId, groupId);
        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetId)
                .orElseThrow(() -> new IllegalArgumentException("目标用户不在群组中"));
        if (target.getRole() == MemberRole.OWNER) throw new IllegalArgumentException("不能踢出群主");
        // admin cannot kick another admin unless they are owner
        GroupMember manager = groupMemberRepository.findByGroupIdAndUserId(groupId, managerId).orElseThrow();
        if (manager.getRole() == MemberRole.ADMIN && target.getRole() == MemberRole.ADMIN)
            throw new IllegalArgumentException("管理员不能踢出其他管理员");
        groupMemberRepository.delete(target);
        Group group = getGroupOrThrow(groupId);
        messagingTemplate.convertAndSendToUser(
            String.valueOf(targetId), "/queue/notifications",
            new NotificationDto("GROUP_KICKED", Map.of("groupId", groupId, "groupName", group.getName())));
    }

    @Transactional
    public void setAdmin(Long ownerId, Long groupId, Long targetId, boolean grant) {
        Group group = getGroupOrThrow(groupId);
        if (!group.getOwnerId().equals(ownerId)) throw new IllegalArgumentException("只有群主可以设置管理员");
        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetId)
                .orElseThrow(() -> new IllegalArgumentException("目标用户不在群组中"));
        if (target.getRole() == MemberRole.OWNER) throw new IllegalArgumentException("不能修改群主角色");
        target.setRole(grant ? MemberRole.ADMIN : MemberRole.MEMBER);
        groupMemberRepository.save(target);
    }

    @Transactional(readOnly = true)
    public List<GroupDto.GroupInfo> getMyGroups(Long userId) {
        return groupMemberRepository.findByUserId(userId).stream()
                .map(m -> toInfo(m.getGroup()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupDto.MemberInfo> getMembers(Long groupId) {
        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(m -> {
                    GroupDto.MemberInfo info = new GroupDto.MemberInfo();
                    info.setUserId(m.getUserId());
                    info.setRole(m.getRole().name());
                    info.setStatus(onlineStatusService.isOnline(m.getUserId()) ? "ONLINE" : "OFFLINE");
                    userRepository.findById(m.getUserId()).ifPresent(u -> {
                        info.setUsername(u.getUsername());
                        info.setAvatar(u.getAvatar());
                    });
                    return info;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<GroupDto.JoinRequestInfo> getPendingRequests(Long managerId, Long groupId) {
        assertOwnerOrAdmin(managerId, groupId);
        return joinRequestRepository.findByGroupIdAndStatus(groupId, GroupJoinRequest.RequestStatus.PENDING)
                .stream()
                .map(req -> {
                    GroupDto.JoinRequestInfo info = new GroupDto.JoinRequestInfo();
                    info.setId(req.getId());
                    info.setGroupId(req.getGroupId());
                    info.setUserId(req.getUserId());
                    info.setStatus(req.getStatus().name());
                    info.setCreatedAt(req.getCreatedAt().toString());
                    userRepository.findById(req.getUserId()).ifPresent(u -> {
                        info.setUsername(u.getUsername());
                        info.setUserAvatar(u.getAvatar());
                    });
                    return info;
                })
                .collect(Collectors.toList());
    }

    private Group getGroupOrThrow(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
    }

    private void assertOwnerOrAdmin(Long userId, Long groupId) {
        GroupMember m = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("不在群组中"));
        if (m.getRole() == MemberRole.MEMBER) throw new IllegalArgumentException("权限不足");
    }

    private boolean isAdmin(Long userId, Long groupId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .map(m -> m.getRole() == MemberRole.OWNER || m.getRole() == MemberRole.ADMIN)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public GroupDto.GroupInfo getGroupInfo(Long groupId) {
        return toInfo(getGroupOrThrow(groupId));
    }

    @Transactional(readOnly = true)
    public GroupDto.GroupInfo toInfo(Group group) {
        GroupDto.GroupInfo info = new GroupDto.GroupInfo();
        info.setId(group.getId());
        info.setName(group.getName());
        info.setOwnerId(group.getOwnerId());
        info.setAvatar(group.getAvatar());
        info.setAnnouncement(group.getAnnouncement());
        info.setMemberCount(groupMemberRepository.findByGroupId(group.getId()).size());
        info.setCreatedAt(group.getCreatedAt().toString());
        userRepository.findById(group.getOwnerId()).ifPresent(u -> info.setOwnerName(u.getUsername()));
        return info;
    }

    @Transactional
    public GroupAnnouncement createAnnouncement(Long userId, Long groupId, GroupDto.AnnouncementRequest req) {
        if (!isAdmin(userId, groupId)) {
            throw new IllegalArgumentException("权限不足");
        }
        GroupAnnouncement announcement = new GroupAnnouncement();
        announcement.setGroupId(groupId);
        announcement.setAuthorId(userId);
        announcement.setContent(req.getContent());
        return announcementRepository.save(announcement);
    }

    @Transactional(readOnly = true)
    public List<GroupDto.AnnouncementInfo> getAnnouncements(Long groupId) {
        return announcementRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                .map(this::toAnnouncementInfo)
                .collect(Collectors.toList());
    }

    private GroupDto.AnnouncementInfo toAnnouncementInfo(GroupAnnouncement announcement) {
        GroupDto.AnnouncementInfo info = new GroupDto.AnnouncementInfo();
        info.setId(announcement.getId());
        info.setGroupId(announcement.getGroupId());
        info.setAuthorId(announcement.getAuthorId());
        info.setContent(announcement.getContent());
        info.setCreatedAt(announcement.getCreatedAt().toString());
        userRepository.findById(announcement.getAuthorId())
                .ifPresent(u -> info.setAuthorName(u.getUsername()));
        return info;
    }
}
