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
 * Controlador encargado de manejar el inicio de sesión simulado.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Player player = authService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(player);
        } catch (IllegalArgumentException e) {
            // Si el mensaje indica que el usuario ya está conectado, devolvemos 409 Conflict
            if (e.getMessage() != null && e.getMessage().contains("ya conectado")) {
                return ResponseEntity.status(409).body(e.getMessage());
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // logout endpoint; frontend puede llamarlo en beforeunload o al hacer logout explícito
    @PostMapping("/logout/{username}")
    public ResponseEntity<?> logout(@org.springframework.web.bind.annotation.PathVariable String username) {
        boolean ok = authService.logout(username);
        if (ok)
            return ResponseEntity.ok().build();
        return ResponseEntity.notFound().build();
    }
}
