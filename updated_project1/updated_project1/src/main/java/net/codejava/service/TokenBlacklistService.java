package net.codejava.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class TokenBlacklistService {
    private final StringRedisTemplate redis;
    public TokenBlacklistService(StringRedisTemplate redis) { this.redis = redis; }

    public void blacklist(String key, long secondsToLive) {
        redis.opsForValue().set(key, "1", Duration.ofSeconds(secondsToLive));
    }
    public boolean isBlacklisted(String key) {
        Boolean exists = redis.hasKey(key);
        return exists != null && exists;
    }
}
