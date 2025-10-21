package com.arsw.shipwreckeds.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.LoginRequest;
import com.arsw.shipwreckeds.service.AuthService;

/**
 * Controlador encargado de manejar el inicio de sesión simulado.
 * 
 * No hay autenticación real, simplemente se valida que el usuario
 * haya ingresado nombre y contraseña, y que no se repita el nombre.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Permite llamadas desde el front
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint para el inicio de sesión simple.
     * 
     * @param request contiene username y password
     * @return Player creado o mensaje de error
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Player player = authService.login(request.getUsername(), request.getPassword());
            return ResponseEntity.ok(player);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
