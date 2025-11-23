package com.arsw.shipwreckeds.service.session;

import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CognitoTokens;

/**
 * Abstraction over the persistence layer used to keep track of logged-in
 * players across multiple backend instances.
 */
public interface PlayerSessionStore {

    long DEFAULT_TTL_SECONDS = 3600;

    /**
     * @return {@code true} when the username currently has an active session.
     */
    boolean hasActiveSession(String username);

    /**
     * Persists a new session using the provided TTL.
     *
     * @return {@code true} if the session was stored, {@code false} if a session already exists.
     */
    boolean createSession(Player player, CognitoTokens tokens, long ttlSeconds);

    /**
     * Retrieves the {@link Player} associated with the username if the session is still valid.
     */
    Player getPlayer(String username);

    /**
     * Returns the Cognito tokens for the active session, if any.
     */
    CognitoTokens getTokens(String username);

    /**
     * Deletes the stored session for the username.
     */
    boolean deleteSession(String username);
}
