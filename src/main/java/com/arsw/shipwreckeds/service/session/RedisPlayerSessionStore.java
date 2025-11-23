package com.arsw.shipwreckeds.service.session;

import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CognitoTokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Redis-backed implementation that keeps track of authenticated players so that
 * any backend instance can validate whether a user is logged in.
 */
@Component
public class RedisPlayerSessionStore implements PlayerSessionStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisPlayerSessionStore.class);
    private static final String KEY_PREFIX = "shipwreckeds:session:";

    private final RedisTemplate<String, PlayerSessionPayload> redisTemplate;
    private final Map<String, PlayerSessionPayload> fallbackSessions = new ConcurrentHashMap<>();

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
        try {
            Boolean stored = redisTemplate.opsForValue().setIfAbsent(keyFor(player.getUsername()), payload,
                    Duration.ofSeconds(effectiveTtl));
            if (Boolean.TRUE.equals(stored)) {
                fallbackSessions.remove(player.getUsername());
                return true;
            }
            return false;
        } catch (DataAccessException | IllegalStateException ex) {
            LOGGER.warn("Redis no disponible para registrar sesión. Usando almacenamiento local temporal: {}",
                    ex.getMessage());
            return storeFallback(player.getUsername(), payload);
        }
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
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(keyFor(username)));
        } catch (DataAccessException | IllegalStateException ex) {
            LOGGER.warn("Redis no disponible para eliminar sesión. Se limpia almacenamiento local: {}",
                    ex.getMessage());
            return fallbackSessions.remove(username) != null;
        }
    }

    private PlayerSessionPayload getPayload(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        try {
            PlayerSessionPayload payload = readFromRedis(username);
            if (payload != null) {
                return payload;
            }
        } catch (DataAccessException | IllegalStateException ex) {
            LOGGER.warn("Redis no disponible para leer sesión. Usando almacenamiento local temporal: {}",
                    ex.getMessage());
        }
        return readFromFallback(username);
    }

    private long nowEpochSeconds() {
        return Instant.now().getEpochSecond();
    }

    private String keyFor(String username) {
        return KEY_PREFIX + username;
    }

    private PlayerSessionPayload readFromRedis(String username) {
        String key = keyFor(username);
        PlayerSessionPayload payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            return null;
        }
        if (payload.isExpired()) {
            redisTemplate.delete(key);
            return null;
        }
        fallbackSessions.remove(username);
        return payload;
    }

    private boolean storeFallback(String username, PlayerSessionPayload payload) {
        PlayerSessionPayload previous = fallbackSessions.putIfAbsent(username, payload);
        return previous == null;
    }

    private PlayerSessionPayload readFromFallback(String username) {
        PlayerSessionPayload payload = fallbackSessions.get(username);
        if (payload == null) {
            return null;
        }
        if (payload.isExpired()) {
            fallbackSessions.remove(username);
            return null;
        }
        return payload;
    }
}
