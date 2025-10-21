package com.arsw.shipwreckeds.service;


import com.arsw.shipwreckeds.model.Player;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Servicio que gestiona el inicio de sesión simulado.
 * 
 * Los jugadores se almacenan temporalmente en memoria.
 * No hay persistencia ni autenticación real.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */
@Service
public class AuthService {

    private final Map<String, Player> loggedPlayers = new HashMap<>();
    private static long nextId = 1;

    /**
     * Intenta iniciar sesión con el nombre y contraseña ingresados.
     * 
     * @param username nombre de usuario ingresado
     * @param password contraseña ingresada
     * @return Player asociado al nombre
     * @throws IllegalArgumentException si hay errores de validación
     */
    public Player login(String username, String password) {
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Por favor ingresa tu nombre y contraseña para continuar.");
        }

        if (loggedPlayers.containsKey(username)) {
            throw new IllegalArgumentException("Este nombre ya está en uso.");
        }

        Player player = new Player(nextId++, username, "default-skin", null);
        loggedPlayers.put(username, player);


        System.out.println("Jugador conectado: " + username);
        return player;
    }
}
