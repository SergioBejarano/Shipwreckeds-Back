package com.arsw.shipwreckeds.controller;

import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.LoginRequest;
import com.arsw.shipwreckeds.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes login and logout endpoints for the multiplayer
 * lobby.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    /**
     * Creates a controller that exposes authentication endpoints backed by
     * {@link AuthService}.
     *
     * @param authService service responsible for validating and tracking logged
     *                    players
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Attempts to authenticate the provided credentials and returns the associated
     * player.
     *
     * @param request payload with username and password
     * @return {@link ResponseEntity} containing the player or an error status if
     *         the login fails
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Player player = authService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(player);
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("ya conectado")) {
                return ResponseEntity.status(409).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Invalidates the active session for the given username if present.
     *
     * @param username identifier of the player to disconnect
     * @return {@link ResponseEntity} indicating whether the player was found
     */
    @PostMapping("/logout/{username}")
    public ResponseEntity<?> logout(@org.springframework.web.bind.annotation.PathVariable String username) {
        boolean ok = authService.logout(username);
        if (ok)
            return ResponseEntity.ok().build();
        return ResponseEntity.notFound().build();
    }
}
