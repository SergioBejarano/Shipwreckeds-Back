package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.model.Player;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Servicio que gestiona el inicio de sesión simulado.
 *
 * Los jugadores se almacenan temporalmente en memoria.
 * No hay persistencia ni autenticación real (MVP).
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
     * Intenta iniciar sesión con el nombre y contraseña ingresados.
     * Rechaza el login si el username ya tiene una sesión activa.
     *
     * @param username nombre de usuario
     * @param password contraseña
     * @return Player creado para la sesión
     * @throws IllegalArgumentException si credenciales inválidas o usuario ya conectado
     */
    public Player login(String username, String password) {
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Por favor ingresa tu nombre y contraseña para continuar.");
        }

        // validar contra usuarios predefinidos (epica 1)
        // sólo se permiten estas cuentas en este MVP
        var allowed = Map.of(
                "ana", "1234",
                "bruno", "1234",
                "carla", "1234",
                "diego", "1234",
                "eva", "1234");

        String expected = allowed.get(username);
        if (expected == null || !expected.equals(password)) {
            throw new IllegalArgumentException("Credenciales inválidas.");
        }

        // Intento atómico de inserción: si ya hay una sesión activa para ese username, rechazamos.
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
     * Retorna el Player actualmente logueado por su username.
     *
     * @param username nombre del jugador
     * @return Player si está conectado, o null si no existe
     */
    public Player getPlayer(String username) {
        if (username == null)
            return null;
        return loggedPlayers.get(username);
    }

    /**
     * Elimina al jugador de la lista de conectados (opcional, útil para pruebas).
     *
     * @param username nombre del jugador a desconectar
     * @return true si existía y fue removido, false si no existía
     */
    public boolean logout(String username) {
        if (username == null)
            return false;
        return loggedPlayers.remove(username) != null;
    }
}
