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

        // initialize positions for players (random inside island)
        double islandRadius = 100.0;
        java.util.Random rnd = new java.util.Random();
        for (Player p : match.getPlayers()) {
            if (p.getPosition() == null) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                double r = rnd.nextDouble() * (islandRadius * 0.7);
                double x = Math.cos(ang) * r;
                double y = Math.sin(ang) * r;
                p.setPosition(new com.arsw.shipwreckeds.model.Position(x, y));
            }
        }
        // ensure npcs have position (NpcService should have set them but double-check)
        for (com.arsw.shipwreckeds.model.Npc n : match.getNpcs()) {
            if (n.getPosition() == null) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                double r = rnd.nextDouble() * (islandRadius * 0.7);
                double x = Math.cos(ang) * r;
                double y = Math.sin(ang) * r;
                n.setPosition(new com.arsw.shipwreckeds.model.Position(x, y));
            }
        }

        // broadcast final lobby and initial game state
        webSocketController.broadcastLobbyUpdate(match);

        // build GameState DTO and broadcast to /topic/game/{code}
        // reuse logic similar to GameController.buildGameState
        java.util.List<com.arsw.shipwreckeds.model.dto.AvatarState> avatars = new java.util.ArrayList<>();
        for (Player p : match.getPlayers()) {
            com.arsw.shipwreckeds.model.Position pos = p.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            avatars.add(new com.arsw.shipwreckeds.model.dto.AvatarState(p.getId(), "human", p.getUsername(), x, y,
                    p.isInfiltrator(), p.isAlive()));
        }
        for (com.arsw.shipwreckeds.model.Npc n : match.getNpcs()) {
            com.arsw.shipwreckeds.model.Position pos = n.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            avatars.add(new com.arsw.shipwreckeds.model.dto.AvatarState(n.getId(), "npc", null, x, y, false, true));
        }
        com.arsw.shipwreckeds.model.dto.GameState.Island isl = new com.arsw.shipwreckeds.model.dto.GameState.Island(0.0,
                0.0, islandRadius);
        com.arsw.shipwreckeds.model.dto.GameState gs = new com.arsw.shipwreckeds.model.dto.GameState(match.getCode(),
                System.currentTimeMillis(), isl, avatars);
        webSocketController.broadcastGameState(match.getCode(), gs);

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
