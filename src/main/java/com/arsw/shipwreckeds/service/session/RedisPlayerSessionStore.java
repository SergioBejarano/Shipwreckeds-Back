package com.arsw.shipwreckeds.service.session;

import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CognitoTokens;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Redis-backed implementation that keeps track of authenticated players so that
 * any backend instance can validate whether a user is logged in.
 */
@Component
public class RedisPlayerSessionStore implements PlayerSessionStore {

    private static final String KEY_PREFIX = "shipwreckeds:session:";

    private final RedisTemplate<String, PlayerSessionPayload> redisTemplate;

    public RedisPlayerSessionStore(
            @Qualifier("playerSessionRedisTemplate") RedisTemplate<String, PlayerSessionPayload> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean hasActiveSession(String username) {
        return getPayload(username) != null;
    }

    @Override
    public boolean createSession(Player player, CognitoTokens tokens, long ttlSeconds) {
        if (player == null || player.getUsername() == null) {
            return false;
        }
        long effectiveTtl = ttlSeconds > 0 ? ttlSeconds : DEFAULT_TTL_SECONDS;
        PlayerSessionPayload payload = new PlayerSessionPayload(player, tokens, nowEpochSeconds(), effectiveTtl);
        Boolean stored = redisTemplate.opsForValue()
                .setIfAbsent(keyFor(player.getUsername()), payload, Duration.ofSeconds(effectiveTtl));
        return Boolean.TRUE.equals(stored);
    }

    @Override
    public Player getPlayer(String username) {
        PlayerSessionPayload payload = getPayload(username);
        return payload != null ? payload.getPlayer() : null;
    }

    @Override
    public CognitoTokens getTokens(String username) {
        PlayerSessionPayload payload = getPayload(username);
        return payload != null ? payload.getTokens() : null;
    }

    @Override
    public boolean deleteSession(String username) {
        if (username == null) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.delete(keyFor(username)));
    }

    private PlayerSessionPayload getPayload(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String key = keyFor(username);
        PlayerSessionPayload payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            return null;
        }
        if (payload.isExpired()) {
            redisTemplate.delete(key);
            return null;
        }
        return payload;
    }

    private long nowEpochSeconds() {
        return Instant.now().getEpochSecond();
    }

    private String keyFor(String username) {
        return KEY_PREFIX + username;
    }
}
