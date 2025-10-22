package com.arsw.shipwreckeds.controller;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CreateMatchRequest;
import com.arsw.shipwreckeds.model.dto.CreateMatchResponse;
import com.arsw.shipwreckeds.model.dto.JoinMatchRequest;
import com.arsw.shipwreckeds.service.MatchService;
import com.arsw.shipwreckeds.service.AuthService;
import com.arsw.shipwreckeds.controller.WebSocketController;
import com.arsw.shipwreckeds.service.RoleService;
import com.arsw.shipwreckeds.service.NpcService;
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
    private final WebSocketController webSocketController;
    private final RoleService roleService;
    private final NpcService npcService;

    public MatchController(MatchService matchService, AuthService authService,
            WebSocketController webSocketController,
            RoleService roleService,
            NpcService npcService) {
        this.matchService = matchService;
        this.authService = authService;
        this.webSocketController = webSocketController;
        this.roleService = roleService;
        this.npcService = npcService;
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
            // broadcast initial lobby state
            webSocketController.broadcastLobbyUpdate(matchService.getMatchByCode(res.getCode()));
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
            // broadcast updated lobby state
            webSocketController.broadcastLobbyUpdate(match);
            return ResponseEntity.ok(match);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/start/{code}")
    public ResponseEntity<?> startMatch(@PathVariable String code, @RequestParam String hostName) {
        Match match = matchService.getMatchByCode(code);
        if (match == null)
            return ResponseEntity.badRequest().body("Partida no encontrada.");

        // simple host check: the first player is the host when created
        if (match.getPlayers().isEmpty() || !match.getPlayers().get(0).getUsername().equals(hostName)) {
            return ResponseEntity.status(403).body("Solo el host puede iniciar la partida.");
        }

        if (match.getPlayers().size() < 5) {
            return ResponseEntity.badRequest()
                    .body("No hay suficientes jugadores para iniciar la partida. Se requieren 5 jugadores humanos.");
        }

        // assign roles and npcs
        roleService.assignHumanRoles(match);
        npcService.generateNpcs(match);
        match.startMatch();

        // broadcast final lobby/game-start state
        webSocketController.broadcastLobbyUpdate(match);

        return ResponseEntity.ok(match);
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getMatch(@PathVariable String code) {
        Match m = matchService.getMatchByCode(code);
        if (m == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(m);
    }
}
