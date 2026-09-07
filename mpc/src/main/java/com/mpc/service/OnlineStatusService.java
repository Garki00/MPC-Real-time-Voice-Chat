package com.mpc.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OnlineStatusService {

    private static final String KEY_PREFIX = "online:";
    private final RedisTemplate<String, Object> redisTemplate;

    public void setOnline(Long userId) {
        redisTemplate.opsForValue().set(KEY_PREFIX + userId, "1", 7, TimeUnit.DAYS);
    }

    public void setOffline(Long userId) {
        redisTemplate.delete(KEY_PREFIX + userId);
    }

    public boolean isOnline(Long userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + userId));
    }
}
