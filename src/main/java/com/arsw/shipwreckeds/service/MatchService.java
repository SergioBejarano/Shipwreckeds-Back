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
 * Servicio que gestiona partidas en memoria para el MVP.
 * - Genera códigos alfanuméricos únicos
 * - Mantiene partidas activas en ConcurrentHashMap
 * - Verifica caducidad en el momento de join
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
    // TTL en segundos para lobbies no iniciados (por ejemplo 2 horas)
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

    public CreateMatchResponse createMatch(Player host) {
        // generar código único
        String code;
        int tries = 0;
        do {
            code = generateCode(CODE_LENGTH);
            tries++;
            if (tries > 50) {
                throw new IllegalArgumentException("No se pudo generar un código único. Intenta de nuevo.");
            }
        } while (matchesByCode.containsKey(code));

        // crear Match y agregar host
        Match match = new Match(nextId++, code);
        match.addPlayer(host);

        // opcional: generar 3 NPCs con skin igual al infiltrado? (infiltrador se asignará al start)
        // Aquí no añadimos NPCs todavía.

        StoredMatch sm = new StoredMatch(match, Instant.now().getEpochSecond(), MATCH_TTL_SECONDS);
        matchesByCode.put(code, sm);

        return new CreateMatchResponse(code);
    }

    public Match joinMatch(String code, Player player) {
        if (code == null || code.trim().isEmpty()) throw new IllegalArgumentException("Código inválido.");
        StoredMatch sm = matchesByCode.get(code);
        if (sm == null) throw new IllegalArgumentException("Código inválido o partida no encontrada.");

        // verificar caducidad
        long now = Instant.now().getEpochSecond();
        if (now > sm.createdAtEpochSec + sm.ttlSeconds) {
            // eliminar y rechazar
            matchesByCode.remove(code);
            throw new IllegalArgumentException("El código ha caducado.");
        }

        Match match = sm.match;

        synchronized (match) {
            if (match.getStatus() != null && match.getStatus().name().equals("STARTED")) {
                throw new IllegalArgumentException("La partida ya ha comenzado.");
            }
            if (match.getPlayers().size() >= 5) {
                throw new IllegalArgumentException("La partida está llena.");
            }
            // evitar nombres duplicados en la misma partida
            boolean nameTaken = match.getPlayers().stream()
                    .anyMatch(p -> p.getUsername().equals(player.getUsername()));
            if (nameTaken) {
                throw new IllegalArgumentException("Ya hay un jugador con ese nombre en la partida.");
            }

            match.addPlayer(player);
        }

        return match;
    }

    public Match getMatchByCode(String code) {
        StoredMatch sm = matchesByCode.get(code);
        if (sm == null) return null;
        long now = Instant.now().getEpochSecond();
        if (now > sm.createdAtEpochSec + sm.ttlSeconds) {
            matchesByCode.remove(code);
            return null;
        }
        return sm.match;
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(ALPHANUM.length());
            sb.append(ALPHANUM.charAt(idx));
        }
        return sb.toString();
    }
}
