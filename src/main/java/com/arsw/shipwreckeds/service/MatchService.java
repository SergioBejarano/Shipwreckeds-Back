package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CreateMatchResponse;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory match registry used by the MVP backend.
 * <ul>
 * <li>Generates unique alphanumeric codes.</li>
 * <li>Stores active matches in a {@link ConcurrentHashMap}.</li>
 * <li>Validates lobby expiration on join requests.</li>
 * </ul>
 *
 * @author Daniel Ruge
 * @version 22/10/2025
 */
@Service
public class MatchService {

    private final Map<String, StoredMatch> matchesByCode = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final long MATCH_TTL_SECONDS = 2 * 60 * 60;

    private static long nextId = 1L;

    private static class StoredMatch {
        Match match;
        long createdAtEpochSec;
        long ttlSeconds;

        StoredMatch(Match match, long createdAtEpochSec, long ttlSeconds) {
            this.match = match;
            this.createdAtEpochSec = createdAtEpochSec;
            this.ttlSeconds = ttlSeconds;
        }
    }

    /**
     * Creates a new match for the host player and registers it under a unique code.
     *
     * @param host player that owns the lobby
     * @return response DTO containing the generated match code
     */
    public CreateMatchResponse createMatch(Player host) {
        // Generate a unique code
        String code;
        int tries = 0;
        do {
            code = generateCode(CODE_LENGTH);
            tries++;
            if (tries > 50) {
                throw new IllegalArgumentException("No se pudo generar un código único. Intenta de nuevo.");
            }
        } while (matchesByCode.containsKey(code));

        // Create the match and add the host
        Match match = new Match(nextId++, code);
        match.addPlayer(host);

        // Optional future step: pre-generate NPCs — deferred until the match actually
        // starts

        StoredMatch sm = new StoredMatch(match, Instant.now().getEpochSecond(), MATCH_TTL_SECONDS);
        matchesByCode.put(code, sm);

        return new CreateMatchResponse(code);
    }

    /**
     * Adds a player to the match identified by the given code if the lobby is still
     * open.
     *
     * @param code   match code provided by the host
     * @param player player attempting to join
     * @return updated {@link Match} instance reflecting the current lobby state
     */
    public Match joinMatch(String code, Player player) {
        if (code == null || code.trim().isEmpty())
            throw new IllegalArgumentException("Código inválido.");
        StoredMatch sm = matchesByCode.get(code);
        if (sm == null)
            throw new IllegalArgumentException("Código inválido o partida no encontrada.");

        // Check whether the lobby has expired
        long now = Instant.now().getEpochSecond();
        if (now > sm.createdAtEpochSec + sm.ttlSeconds) {
            // Remove the lobby and reject the join attempt
            matchesByCode.remove(code);
            throw new IllegalArgumentException("El código ha caducado.");
        }

        Match match = sm.match;

        synchronized (match) {
            if (match.getStatus() != null && match.getStatus().name().equals("STARTED")) {
                throw new IllegalArgumentException("La partida ya ha comenzado.");
            }
            if (match.getPlayers().size() >= 8) {
                throw new IllegalArgumentException("La partida está llena.");
            }
            // Prevent duplicate usernames inside the same match
            boolean nameTaken = match.getPlayers().stream()
                    .anyMatch(p -> p.getUsername().equals(player.getUsername()));
            if (nameTaken) {
                throw new IllegalArgumentException("Ya hay un jugador con ese nombre en la partida.");
            }

            match.addPlayer(player);
        }

        return match;
    }

    /**
     * Retrieves the match referenced by the given code, pruning expired entries if
     * necessary.
     *
     * @param code match identifier assigned at creation time
     * @return match instance or {@code null} if not found or expired
     */
    public Match getMatchByCode(String code) {
        StoredMatch sm = matchesByCode.get(code);
        if (sm == null)
            return null;
        long now = Instant.now().getEpochSecond();
        if (now > sm.createdAtEpochSec + sm.ttlSeconds) {
            matchesByCode.remove(code);
            return null;
        }
        return sm.match;
    }

    /**
     * Generates an alphanumeric code of the requested length using
     * {@link SecureRandom}.
     *
     * @param length number of characters to generate
     * @return new alphanumeric code
     */
    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(ALPHANUM.length());
            sb.append(ALPHANUM.charAt(idx));
        }
        return sb.toString();
    }
}
