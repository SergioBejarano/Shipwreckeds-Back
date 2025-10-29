package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.model.Player;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory authentication service used for the MVP lobby.
 * <p>
 * Players are tracked in memory only; there is no persistent storage or real
 * authentication backend.
 *
 * @author Daniel Ruge
 * @version 19/10/2025
 */
@Service
public class AuthService {

    // ConcurrentHashMap para seguridad en concurrencia (múltiples requests)
    private final Map<String, Player> loggedPlayers = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    /**
     * Attempts to authenticate the supplied credentials and allocate a new
     * in-memory player session.
     * The login is rejected if the username already has an active session.
     *
     * @param username player identifier attempting to log in
     * @param password plain-text password to validate against the predefined
     *                 accounts
     * @return {@link Player} representing the active session
     * @throws IllegalArgumentException if the credentials are missing, invalid, or
     *                                  the player is already connected
     */
    public Player login(String username, String password) {
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Por favor ingresa tu nombre y contraseña para continuar.");
        }

        // Validate against predefined accounts; only these credentials are accepted in
        // this MVP
        var allowed = Map.of(
                "ana", "1234",
                "bruno", "1234",
                "carla", "1234",
                "diego", "1234",
                "eva", "1234",
                "fran", "1234",
                "galo", "1234",
                "helen", "1234");

        String expected = allowed.get(username);
        if (expected == null || !expected.equals(password)) {
            throw new IllegalArgumentException("Credenciales inválidas.");
        }

        // Atomic insertion attempt: reject if the username already has an active
        // session
        Player candidate = new Player(nextId.getAndIncrement(), username, "default-skin", null);
        Player previous = loggedPlayers.putIfAbsent(username, candidate);
        if (previous != null) {
            // Ya había alguien conectado con ese nombre
            throw new IllegalArgumentException("Usuario ya conectado desde otro cliente.");
        }

        System.out.println("Jugador conectado: " + username);
        return candidate;
    }

    /**
     * Retrieves the logged-in {@link Player} with the given username, if present.
     *
     * @param username unique player identifier
     * @return player if connected, or {@code null} when no session exists for that
     *         username
     */
    public Player getPlayer(String username) {
        if (username == null)
            return null;
        return loggedPlayers.get(username);
    }

    /**
     * Removes the player from the active session registry.
     *
     * @param username identifier of the player to disconnect
     * @return {@code true} if a session was removed, otherwise {@code false}
     */
    public boolean logout(String username) {
        if (username == null)
            return false;
        return loggedPlayers.remove(username) != null;
    }
}
