package com.mpc.controller;

import com.mpc.dto.VoiceChannelDto;
import com.mpc.model.User;
import com.mpc.service.VoiceChannelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class VoiceChannelController {

    private final VoiceChannelService voiceChannelService;

    @PostMapping("/api/groups/{groupId}/channels")
    public ResponseEntity<VoiceChannelDto.ChannelInfo> create(
            @AuthenticationPrincipal User user,
            @PathVariable Long groupId,
            @Valid @RequestBody VoiceChannelDto.CreateRequest req) {
        return ResponseEntity.ok(voiceChannelService.createChannel(user.getId(), groupId, req));
    }

    @PutMapping("/api/channels/{channelId}")
    public ResponseEntity<VoiceChannelDto.ChannelInfo> update(
            @AuthenticationPrincipal User user,
            @PathVariable Long channelId,
            @Valid @RequestBody VoiceChannelDto.UpdateRequest req) {
        return ResponseEntity.ok(voiceChannelService.updateChannel(user.getId(), channelId, req));
    }

    @DeleteMapping("/api/channels/{channelId}")
    public ResponseEntity<String> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long channelId) {
        voiceChannelService.deleteChannel(user.getId(), channelId);
        return ResponseEntity.ok("语音分组已删除");
    }

    @GetMapping("/api/groups/{groupId}/channels")
    public ResponseEntity<List<VoiceChannelDto.ChannelInfo>> list(@PathVariable Long groupId) {
        return ResponseEntity.ok(voiceChannelService.getChannels(groupId));
    }

    // STOMP: join voice channel
    @MessageMapping("/voice.join")
    public void joinChannel(Principal principal, @Payload Map<String, Long> body) {
        Long userId = Long.parseLong(principal.getName());
        voiceChannelService.joinChannel(userId, body.get("channelId"));
    }

    // STOMP: leave voice channel
    @MessageMapping("/voice.leave")
    public void leaveChannel(Principal principal, @Payload Map<String, Long> body) {
        Long userId = Long.parseLong(principal.getName());
        voiceChannelService.leaveChannel(userId, body.get("channelId"));
    }
}
