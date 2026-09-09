package com.mpc.service;

import com.mpc.dto.VoiceChannelDto;
import com.mpc.model.GroupMember;
import com.mpc.model.VoiceChannel;
import com.mpc.repository.GroupMemberRepository;
import com.mpc.repository.GroupRepository;
import com.mpc.repository.UserRepository;
import com.mpc.repository.VoiceChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VoiceChannelService {

    private static final String VOICE_KEY = "voice:channel:";
    private static final String USER_CHANNEL_KEY = "voice:user:";

    private final VoiceChannelRepository voiceChannelRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public VoiceChannelDto.ChannelInfo createChannel(Long userId, Long groupId, VoiceChannelDto.CreateRequest req) {
        assertOwnerOrAdmin(userId, groupId);
        if (req.getMaxCapacity() > 50) throw new IllegalArgumentException("语音频道上限最多50人");
        VoiceChannel vc = new VoiceChannel();
        vc.setGroup(groupRepository.getReferenceById(groupId));
        vc.setName(req.getName());
        vc.setMaxCapacity(req.getMaxCapacity());
        vc.setCreatedBy(userId);
        return toInfo(voiceChannelRepository.save(vc));
    }

    @Transactional
    public void deleteChannel(Long userId, Long channelId) {
        VoiceChannel vc = getChannelOrThrow(channelId);
        assertOwnerOrAdmin(userId, vc.getGroup().getId());
        // kick everyone out first
        Set<Object> members = redisTemplate.opsForSet().members(VOICE_KEY + channelId);
        if (members != null) {
            members.forEach(uid -> {
                redisTemplate.delete(USER_CHANNEL_KEY + uid);
                messagingTemplate.convertAndSendToUser(
                    String.valueOf(uid), "/queue/notifications",
                    Map.of("type", "VOICE_CHANNEL_DELETED", "channelId", channelId));
            });
        }
        redisTemplate.delete(VOICE_KEY + channelId);
        voiceChannelRepository.delete(vc);
        broadcastChannelList(vc.getGroup().getId());
    }

    @Transactional
    public VoiceChannelDto.ChannelInfo updateChannel(Long userId, Long channelId, VoiceChannelDto.UpdateRequest req) {
        VoiceChannel vc = getChannelOrThrow(channelId);
        assertOwnerOrAdmin(userId, vc.getGroup().getId());
        if (req.getName() != null) vc.setName(req.getName());
        if (req.getMaxCapacity() != null) {
            if (req.getMaxCapacity() > 50) throw new IllegalArgumentException("语音频道上限最多50人");
            vc.setMaxCapacity(req.getMaxCapacity());
        }
        return toInfo(voiceChannelRepository.save(vc));
    }

    @Transactional
    public void joinChannel(Long userId, Long channelId) {
        VoiceChannel vc = getChannelOrThrow(channelId);
        Long groupId = vc.getGroup().getId();

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId))
            throw new IllegalArgumentException("不在该群组中");

        // leave current channel if any
        String currentKey = USER_CHANNEL_KEY + userId;
        Object current = redisTemplate.opsForValue().get(currentKey);
        if (current != null) {
            leaveChannelInternal(userId, Long.parseLong(current.toString()));
        }

        Long count = redisTemplate.opsForSet().size(VOICE_KEY + channelId);
        if (count != null && count >= vc.getMaxCapacity())
            throw new IllegalArgumentException("语音分组已满");

        redisTemplate.opsForSet().add(VOICE_KEY + channelId, String.valueOf(userId));
        redisTemplate.opsForValue().set(currentKey, String.valueOf(channelId));

        broadcastChannelState(channelId, groupId);
    }

    @Transactional
    public void leaveChannel(Long userId, Long channelId) {
        leaveChannelInternal(userId, channelId);
        VoiceChannel vc = getChannelOrThrow(channelId);
        broadcastChannelState(channelId, vc.getGroup().getId());
    }

    private void leaveChannelInternal(Long userId, Long channelId) {
        redisTemplate.opsForSet().remove(VOICE_KEY + channelId, String.valueOf(userId));
        redisTemplate.delete(USER_CHANNEL_KEY + userId);
    }

    @Transactional(readOnly = true)
    public List<VoiceChannelDto.ChannelInfo> getChannels(Long groupId) {
        return voiceChannelRepository.findByGroupId(groupId).stream()
                .map(vc -> toInfo(vc))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public VoiceChannelDto.ChannelInfo toInfo(VoiceChannel vc) {
        VoiceChannelDto.ChannelInfo info = new VoiceChannelDto.ChannelInfo();
        info.setId(vc.getId());
        info.setGroupId(vc.getGroup().getId());
        info.setName(vc.getName());
        info.setMaxCapacity(vc.getMaxCapacity());

        Set<Object> members = redisTemplate.opsForSet().members(VOICE_KEY + vc.getId());
        List<VoiceChannelDto.ParticipantInfo> participants = members == null ? new ArrayList<>() :
                members.stream()
                    .map(o -> Long.parseLong(o.toString()))
                    .map(uid -> {
                        VoiceChannelDto.ParticipantInfo p = new VoiceChannelDto.ParticipantInfo();
                        p.setUserId(uid);
                        userRepository.findById(uid).ifPresent(u -> {
                            p.setUsername(u.getUsername());
                            p.setAvatar(u.getAvatar());
                        });
                        return p;
                    })
                    .collect(Collectors.toList());
        info.setParticipants(participants);
        info.setCurrentCount(participants.size());
        return info;
    }

    private void broadcastChannelState(Long channelId, Long groupId) {
        VoiceChannel vc = getChannelOrThrow(channelId);
        messagingTemplate.convertAndSend("/topic/voice/" + groupId, toInfo(vc));
    }

    private void broadcastChannelList(Long groupId) {
        messagingTemplate.convertAndSend("/topic/voice/" + groupId + "/list", getChannels(groupId));
    }

    private VoiceChannel getChannelOrThrow(Long channelId) {
        return voiceChannelRepository.findById(channelId)
                .orElseThrow(() -> new IllegalArgumentException("语音分组不存在"));
    }

    private void assertOwnerOrAdmin(Long userId, Long groupId) {
        GroupMember m = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("不在群组中"));
        if (m.getRole() == GroupMember.MemberRole.MEMBER)
            throw new IllegalArgumentException("权限不足");
    }
}
