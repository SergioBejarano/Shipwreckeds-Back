package com.arsw.shipwreckeds.controller;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.Position;
import com.arsw.shipwreckeds.model.dto.CreateMatchRequest;
import com.arsw.shipwreckeds.model.dto.CreateMatchResponse;
import com.arsw.shipwreckeds.model.dto.JoinMatchRequest;
import com.arsw.shipwreckeds.model.dto.AvatarState;
import com.arsw.shipwreckeds.model.dto.GameState;
import com.arsw.shipwreckeds.service.AuthService;
import com.arsw.shipwreckeds.service.GameEngine;
import com.arsw.shipwreckeds.service.MatchService;
import com.arsw.shipwreckeds.service.NpcService;
import com.arsw.shipwreckeds.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Controlador que maneja creación y unión a partidas (lobbies).
 * Implementación limpia y con buenas prácticas de imports y estructura.
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
    private final GameEngine gameEngine;

    public MatchController(MatchService matchService,
            AuthService authService,
            WebSocketController webSocketController,
            RoleService roleService,
            NpcService npcService,
            GameEngine gameEngine) {
        this.matchService = matchService;
        this.authService = authService;
        this.webSocketController = webSocketController;
        this.roleService = roleService;
        this.npcService = npcService;
        this.gameEngine = gameEngine;
    }

    @PostMapping("/create")
    public ResponseEntity<?> createMatch(@RequestBody CreateMatchRequest req) {
        try {
            Player host = authService.getPlayer(req.getHostName());
            if (host == null) {
                return ResponseEntity.badRequest().body("Usuario host no conectado. Inicia sesión primero.");
            }
            CreateMatchResponse res = matchService.createMatch(host);
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
                p.setPosition(new Position(x, y));
            }
        }
        // ensure npcs have position
        for (Npc n : match.getNpcs()) {
            if (n.getPosition() == null) {
                double ang = rnd.nextDouble() * Math.PI * 2;
                double r = rnd.nextDouble() * (islandRadius * 0.7);
                double x = Math.cos(ang) * r;
                double y = Math.sin(ang) * r;
                n.setPosition(new Position(x, y));
            }
        }

        // broadcast final lobby and initial game state
        webSocketController.broadcastLobbyUpdate(match);

        List<AvatarState> avatars = new ArrayList<>();
        for (Player p : match.getPlayers()) {
            Position pos = p.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            if (p.isInfiltrator()) {
                avatars.add(new AvatarState(p.getId(), "npc", null, x, y, false, p.isAlive()));
            } else {
                avatars.add(new AvatarState(p.getId(), "human", p.getUsername(), x, y, false, p.isAlive()));
            }
        }
        for (Npc n : match.getNpcs()) {
            Position pos = n.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            avatars.add(new AvatarState(n.getId(), "npc", null, x, y, false, true));
        }

        GameState.Island isl = new GameState.Island(0.0, 0.0, islandRadius);
        GameState gs = new GameState(match.getCode(), System.currentTimeMillis(), match.getTimerSeconds(), isl,
                avatars);
        webSocketController.broadcastGameState(match.getCode(), gs);

        // start server-side countdown ticker for the match
        gameEngine.startMatchTicker(match);

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
