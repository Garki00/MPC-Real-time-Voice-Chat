package com.mpc.service;

import com.mpc.dto.NotificationDto;
import com.mpc.dto.UserDto;
import com.mpc.model.Friendship;
import com.mpc.model.User;
import com.mpc.repository.FriendshipRepository;
import com.mpc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FriendService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void sendRequest(Long fromId, Long toId) {
        if (fromId.equals(toId)) throw new IllegalArgumentException("不能添加自己为好友");
        userRepository.findById(toId).orElseThrow(() -> new IllegalArgumentException("目标用户不存在"));
        friendshipRepository.findByPair(fromId, toId).ifPresent(f -> {
            if (f.getStatus() == Friendship.FriendshipStatus.ACCEPTED)
                throw new IllegalArgumentException("已经是好友");
            if (f.getStatus() == Friendship.FriendshipStatus.PENDING)
                throw new IllegalArgumentException("已发送过好友申请");
        });
        Friendship f = new Friendship();
        f.setUserId(fromId);
        f.setFriendId(toId);
        friendshipRepository.save(f);

        User from = userRepository.findById(fromId).orElseThrow();
        messagingTemplate.convertAndSendToUser(
                String.valueOf(toId), "/queue/notifications",
                new NotificationDto("FRIEND_REQUEST", userService.toInfo(from)));
    }

    @Transactional
    public void handleRequest(Long currentUserId, Long requesterId, boolean accept) {
        Friendship f = friendshipRepository.findByPair(requesterId, currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("好友申请不存在"));
        if (f.getStatus() != Friendship.FriendshipStatus.PENDING)
            throw new IllegalArgumentException("申请已处理");
        f.setStatus(accept ? Friendship.FriendshipStatus.ACCEPTED : Friendship.FriendshipStatus.REJECTED);
        friendshipRepository.save(f);

        if (accept) {
            User current = userRepository.findById(currentUserId).orElseThrow();
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(requesterId), "/queue/notifications",
                    new NotificationDto("FRIEND_ACCEPTED", userService.toInfo(current)));
        }
    }

    @Transactional
    public void deleteFriend(Long userId, Long friendId) {
        Friendship f = friendshipRepository.findByPair(userId, friendId)
                .orElseThrow(() -> new IllegalArgumentException("好友关系不存在"));
        friendshipRepository.delete(f);
    }

    @Transactional(readOnly = true)
    public List<UserDto.UserInfo> getFriends(Long userId) {
        return friendshipRepository.findAcceptedFriendships(userId).stream()
                .map(f -> {
                    Long friendId = f.getUserId().equals(userId) ? f.getFriendId() : f.getUserId();
                    return userRepository.findById(friendId).map(userService::toInfo).orElse(null);
                })
                .filter(u -> u != null)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto.UserInfo> getPendingRequests(Long userId) {
        return friendshipRepository.findPendingRequests(userId).stream()
                .map(f -> userRepository.findById(f.getUserId()).map(userService::toInfo).orElse(null))
                .filter(u -> u != null)
                .collect(Collectors.toList());
    }
}
