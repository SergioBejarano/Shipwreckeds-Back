package com.arsw.shipwreckeds.service.cache;

import com.arsw.shipwreckeds.model.Match;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Thin wrapper around RedisTemplate that persists {@link Match} snapshots while
 * preserving the original TTL semantics used by the in-memory implementation.
 */
@Component
public class MatchCacheRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MatchCacheRepository.class);
    private static final String KEY_PREFIX = "shipwreckeds:match:";

    private final RedisTemplate<String, MatchCachePayload> redisTemplate;

    public MatchCacheRepository(RedisTemplate<String, MatchCachePayload> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Match match, long ttlSeconds) {
        if (match == null || match.getCode() == null) {
            return;
        }
        MatchCachePayload payload = new MatchCachePayload(match, System.currentTimeMillis() / 1000, ttlSeconds);
        String key = keyFor(match.getCode());
        redisTemplate.opsForValue().set(key, payload, Duration.ofSeconds(ttlSeconds));
    }

    public Match findActive(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        String key = keyFor(code);
        MatchCachePayload payload = redisTemplate.opsForValue().get(key);
        if (payload == null) {
            return null;
        }
        if (payload.isExpired()) {
            redisTemplate.delete(key);
            LOGGER.debug("Expired match {} evicted from cache", code);
            return null;
        }
        return payload.getMatch();
    }

    public void delete(String code) {
        if (code == null) {
            return;
        }
        redisTemplate.delete(keyFor(code));
    }

    private String keyFor(String code) {
        return KEY_PREFIX + code;
    }
}
