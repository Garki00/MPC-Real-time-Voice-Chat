package com.mpc.repository;

import com.mpc.model.VoiceChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VoiceChannelRepository extends JpaRepository<VoiceChannel, Long> {
    List<VoiceChannel> findByGroupId(Long groupId);
    void deleteByGroupId(Long groupId);
}
