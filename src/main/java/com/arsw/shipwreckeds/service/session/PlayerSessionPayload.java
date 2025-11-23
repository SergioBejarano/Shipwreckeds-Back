package com.arsw.shipwreckeds.service.session;

import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CognitoTokens;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.time.Instant;

/**
 * Wrapper stored in Valkey/Redis that contains the serialized {@link Player}
 * plus the Cognito tokens used to authenticate.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerSessionPayload implements Serializable {

    private Player player;
    private CognitoTokens tokens;
    private long createdAtEpochSec;
    private long ttlSeconds;

    public PlayerSessionPayload() {
    }

    public PlayerSessionPayload(Player player, CognitoTokens tokens, long createdAtEpochSec, long ttlSeconds) {
        this.player = player;
        this.tokens = tokens;
        this.createdAtEpochSec = createdAtEpochSec;
        this.ttlSeconds = ttlSeconds;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public CognitoTokens getTokens() {
        return tokens;
    }

    public void setTokens(CognitoTokens tokens) {
        this.tokens = tokens;
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

    @JsonIgnore
    public boolean isExpired() {
        long now = Instant.now().getEpochSecond();
        return ttlSeconds > 0 && now > (createdAtEpochSec + ttlSeconds);
    }
}
