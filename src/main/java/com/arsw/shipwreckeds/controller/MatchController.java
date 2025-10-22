package com.arsw.shipwreckeds.controller;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CreateMatchRequest;
import com.arsw.shipwreckeds.model.dto.CreateMatchResponse;
import com.arsw.shipwreckeds.model.dto.JoinMatchRequest;
import com.arsw.shipwreckeds.service.MatchService;
import com.arsw.shipwreckeds.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador que maneja creación y unión a partidas (lobbies).
 * MVP: almacenamiento en memoria, códigos alfanuméricos únicos y TTL simple.
 * 
 * @author Daniel Ruge
 * @version 22/10/2025
 */
@RestController
@RequestMapping("/api/match")
@CrossOrigin(origins = "*")
public class MatchController {

    private final MatchService matchService;
    private final AuthService authService;

    public MatchController(MatchService matchService, AuthService authService) {
        this.matchService = matchService;
        this.authService = authService;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createMatch(@RequestBody CreateMatchRequest req) {
        try {
            // require that the host is a logged player (from AuthService)
            Player host = authService.getPlayer(req.getHostName());
            if (host == null) {
                return ResponseEntity.badRequest().body("Usuario host no conectado. Inicia sesión primero.");
            }
            CreateMatchResponse res = matchService.createMatch(host);
            return ResponseEntity.ok(res);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/join")
    public ResponseEntity<?> joinMatch(@RequestBody JoinMatchRequest req) {
        try {
            Player player = authService.getPlayer(req.getUsername());
            if (player == null) {
                return ResponseEntity.badRequest().body("Usuario no conectado. Inicia sesión primero.");
            }
            Match match = matchService.joinMatch(req.getCode(), player);
            return ResponseEntity.ok(match);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getMatch(@PathVariable String code) {
        Match m = matchService.getMatchByCode(code);
        if (m == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(m);
    }
}
