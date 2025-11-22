package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CreateMatchResponse;
import com.arsw.shipwreckeds.service.cache.MatchCacheRepository;
import com.arsw.shipwreckeds.service.cache.MatchLockManager;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

/**
 * Distributed match registry backed by AWS Valkey (Redis) so multiple backend
 * instances can share the same mutable game state. Generates unique lobby
 * codes, validates TTL, and executes state mutations under a Redis-backed
 * distributed lock to keep the game logic consistent across nodes.
 */
@Service
public class MatchService {

    private final SecureRandom random = new SecureRandom();
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final long MATCH_TTL_SECONDS = 2 * 60 * 60;

    private static final AtomicLong NEXT_ID = new AtomicLong(1L);

    private final MatchCacheRepository cacheRepository;
    private final MatchLockManager lockManager;

    public MatchService(MatchCacheRepository cacheRepository, MatchLockManager lockManager) {
        this.cacheRepository = cacheRepository;
        this.lockManager = lockManager;
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
        } while (cacheRepository.findActive(code) != null);

        // Create the match and add the host
        Match match = new Match(NEXT_ID.getAndIncrement(), code);
        match.addPlayer(host);
        cacheRepository.save(match, MATCH_TTL_SECONDS);

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
        return updateMatch(code, match -> {
            if (match.getStatus() != null && match.getStatus().name().equals("STARTED")) {
                throw new IllegalArgumentException("La partida ya ha comenzado.");
            }
            if (match.getPlayers().size() >= 8) {
                throw new IllegalArgumentException("La partida está llena.");
            }
            boolean nameTaken = match.getPlayers().stream()
                    .anyMatch(p -> p.getUsername().equals(player.getUsername()));
            if (nameTaken) {
                throw new IllegalArgumentException("Ya hay un jugador con ese nombre en la partida.");
            }

            match.addPlayer(player);
            return match;
        });
    }

    /**
     * Retrieves the match referenced by the given code, pruning expired entries if
     * necessary.
     *
     * @param code match identifier assigned at creation time
     * @return match instance or {@code null} if not found or expired
     */
    public Match getMatchByCode(String code) {
        return cacheRepository.findActive(code);
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

    public void saveMatch(Match match) {
        if (match != null) {
            cacheRepository.save(match, MATCH_TTL_SECONDS);
        }
    }

    public void removeMatch(String code) {
        cacheRepository.delete(code);
    }

    public <T> T updateMatch(String code, Function<Match, T> updater) {
        return lockManager.withLock(code, () -> {
            Match match = cacheRepository.findActive(code);
            if (match == null) {
                throw new IllegalArgumentException("Código inválido o partida no encontrada.");
            }
            T result = updater.apply(match);
            cacheRepository.save(match, MATCH_TTL_SECONDS);
            return result;
        });
    }
}
