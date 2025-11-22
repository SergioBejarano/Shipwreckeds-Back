package com.arsw.shipwreckeds.service.cache;

import com.arsw.shipwreckeds.model.Match;

import java.io.Serializable;
import java.time.Instant;

/**
 * Wrapper stored in Valkey/Redis containing the serialized {@link Match}
 * alongside basic metadata to manage TTL locally.
 */
public class MatchCachePayload implements Serializable {

    private Match match;
    private long createdAtEpochSec;
    private long ttlSeconds;

    public MatchCachePayload() {
    }

    public MatchCachePayload(Match match, long createdAtEpochSec, long ttlSeconds) {
        this.match = match;
        this.createdAtEpochSec = createdAtEpochSec;
        this.ttlSeconds = ttlSeconds;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public long getCreatedAtEpochSec() {
        return createdAtEpochSec;
    }

    public void setCreatedAtEpochSec(long createdAtEpochSec) {
        this.createdAtEpochSec = createdAtEpochSec;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    public boolean isExpired() {
        long now = Instant.now().getEpochSecond();
        return ttlSeconds > 0 && now > (createdAtEpochSec + ttlSeconds);
    }
}
