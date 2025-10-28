package com.arsw.shipwreckeds.controller;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.MatchStatus;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.Position;
import com.arsw.shipwreckeds.model.dto.CreateMatchRequest;
import com.arsw.shipwreckeds.model.dto.CreateMatchResponse;
import com.arsw.shipwreckeds.model.dto.JoinMatchRequest;
import com.arsw.shipwreckeds.model.dto.AvatarState;
import com.arsw.shipwreckeds.model.dto.EliminationEvent;
import com.arsw.shipwreckeds.model.dto.FuelActionRequest;
import com.arsw.shipwreckeds.model.dto.FuelActionResponse;
import com.arsw.shipwreckeds.model.dto.GameState;
import com.arsw.shipwreckeds.model.dto.VoteAck;
import com.arsw.shipwreckeds.model.dto.VoteRequest;
import com.arsw.shipwreckeds.model.dto.VoteResult;
import com.arsw.shipwreckeds.model.dto.VoteStart;
import com.arsw.shipwreckeds.service.AuthService;
import com.arsw.shipwreckeds.service.GameEngine;
import com.arsw.shipwreckeds.service.MatchService;
import com.arsw.shipwreckeds.service.NpcService;
import com.arsw.shipwreckeds.service.RoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controlador que maneja creación y unión a partidas (lobbies).
 * Implementación limpia y con buenas prácticas de imports y estructura.
 */
@RestController
@RequestMapping("/api/match")
@CrossOrigin(origins = "*")
public class MatchController {
    private static final double ISLAND_RADIUS = 100.0;
    private static final double BOAT_X = ISLAND_RADIUS + 12.0;
    private static final double BOAT_Y = 0.0;
    private static final double BOAT_INTERACTION_RADIUS = 40.0;
    private static final double FUEL_STEP = 5.0;
    private static final double ELIMINATION_RANGE = 20.0;

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
                match.resetFuel();
            }
        }

        // broadcast final lobby and initial game state
        webSocketController.broadcastLobbyUpdate(match);

        webSocketController.broadcastGameState(match.getCode(), buildGameStateForMatch(match));

        // start server-side countdown ticker for the match
        gameEngine.startMatchTicker(match);

        return ResponseEntity.ok(match);
    }

    @PostMapping("/{code}/startVote")
    public ResponseEntity<?> startVote(@PathVariable String code, @RequestParam String username) {
        Match match = matchService.getMatchByCode(code);
        if (match == null)
            return ResponseEntity.badRequest().body("Partida no encontrada.");
        if (match.getStatus() != null && match.getStatus().name().equals("STARTED") == false)
            return ResponseEntity.badRequest().body("La partida no está en curso.");

        // ensure player exists and is alive and is NOT the infiltrator
        Player p = match.getPlayers().stream().filter(pl -> pl.getUsername().equals(username)).findFirst().orElse(null);
        if (p == null || !p.isAlive())
            return ResponseEntity.status(403).body("Jugador no válido para iniciar votación.");
        if (p.isInfiltrator())
            return ResponseEntity.status(403).body("El infiltrado no puede iniciar votaciones.");

        if (match.isVotingActive())
            return ResponseEntity.badRequest().body("Ya hay una votación en curso.");

        // Build options: include NPCs and any human players that are disguised as
        // infiltrators
        List<AvatarState> options = new ArrayList<>();
        // NPCs
        for (Npc n : match.getNpcs()) {
            Position pos = n.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            options.add(
                    new AvatarState(n.getId(), "npc", null, x, y, n.isInfiltrator(), n.isActive(), n.getDisplayName()));
        }
        // Players who are infiltrators should also be votable (they appear as NPC on
        // the island)
        for (Player pl : match.getPlayers()) {
            if (pl.isInfiltrator() && pl.isAlive()) {
                Position pos = pl.getPosition();
                double x = pos != null ? pos.getX() : 0.0;
                double y = pos != null ? pos.getY() : 0.0;
                // represent as npc in options (no ownerUsername)
                options.add(new AvatarState(pl.getId(), "npc", null, x, y, true, pl.isAlive(),
                        GameEngine.buildNpcAlias(pl.getId())));
            }
        }

        match.startVoting();
        gameEngine.scheduleVoteTimeout(match, () -> concludeVote(match, code, true));
        // broadcast game state (infiltrators must be represented as NPCs with no
        // ownerUsername)
        webSocketController.broadcastGameState(match.getCode(), buildGameStateForMatch(match));
        VoteStart vs = new VoteStart(options,
                "Iniciar votación: elige un NPC para expulsar",
                Match.VOTE_DURATION_SECONDS);
        webSocketController.broadcastVoteStart(code, vs);
        return ResponseEntity.ok("Votación iniciada");
    }

    @PostMapping("/{code}/vote")
    public ResponseEntity<?> submitVote(@PathVariable String code, @RequestBody VoteRequest req) {
        Match match = matchService.getMatchByCode(code);
        if (match == null)
            return ResponseEntity.badRequest().body("Partida no encontrada.");
        if (!match.isVotingActive())
            return ResponseEntity.badRequest().body("No hay votación activa.");

        // validate voter
        Player voter = match.getPlayers().stream().filter(pl -> pl.getUsername().equals(req.getUsername())).findFirst()
                .orElse(null);
        if (voter == null || !voter.isAlive())
            return ResponseEntity.status(403).body("Jugador no válido para votar.");
        if (voter.isInfiltrator())
            return ResponseEntity.status(403).body("El infiltrado no puede votar.");

        // record vote
        match.recordVote(req.getUsername(), req.getTargetId());

        // ack to voter
        VoteAck ack = new VoteAck(req.getUsername(), "Voto registrado correctamente, esperando resultados finales.");

        // if all humans have voted, compute results
        if (match.allHumansVoted()) {
            concludeVote(match, code, false);
        }

        return ResponseEntity.ok(ack);
    }

    @GetMapping("/{code}")
    public ResponseEntity<?> getMatch(@PathVariable String code) {
        Match m = matchService.getMatchByCode(code);
        if (m == null)
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(m);
    }

    // Helper to build GameState DTO from Match
    private GameState buildGameStateForMatch(Match match) {
        List<AvatarState> avatars = new ArrayList<>();
        for (Player p : match.getPlayers()) {
            Position pos = p.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            String type = p.isInfiltrator() ? "npc" : "human";
            String owner = p.isInfiltrator() ? null : p.getUsername();
            String dname = p.isInfiltrator() ? GameEngine.buildNpcAlias(p.getId()) : p.getUsername();
            avatars.add(new AvatarState(p.getId(), type, owner, x, y, p.isInfiltrator(), p.isAlive(), dname));
        }
        for (Npc n : match.getNpcs()) {
            Position pos = n.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            avatars.add(
                    new AvatarState(n.getId(), "npc", null, x, y, n.isInfiltrator(), n.isActive(), n.getDisplayName()));
        }
        GameState.Island isl = new GameState.Island(0.0, 0.0, ISLAND_RADIUS);
        GameState.Boat boat = new GameState.Boat(BOAT_X, BOAT_Y, BOAT_INTERACTION_RADIUS);
        String status = match.getStatus() != null ? match.getStatus().name() : MatchStatus.WAITING.name();
        return new GameState(
                match.getCode(),
                System.currentTimeMillis(),
                match.getTimerSeconds(),
                isl,
                avatars,
                match.getFuelPercentage(),
                status,
                boat,
                match.getWinnerMessage(),
                match.isFuelWindowOpenNow(),
                match.getFuelWindowSecondsRemaining());

    }

    private void concludeVote(Match match, String code, boolean dueToTimeout) {
        synchronized (match) {
            if (!match.isVotingActive()) {
                return;
            }

            gameEngine.cancelVoteTimeout(code);

            java.util.Map<Long, Integer> counts = new java.util.LinkedHashMap<>();
            if (match.getVotesByPlayer() != null) {
                for (Long tid : match.getVotesByPlayer().values()) {
                    if (tid == null) {
                        continue;
                    }
                    counts.put(tid, counts.getOrDefault(tid, 0) + 1);
                }
            }

            int totalVotes = 0;
            for (java.util.Map.Entry<Long, Integer> entry : counts.entrySet()) {
                Long targetId = entry.getKey();
                Integer value = entry.getValue();
                if (targetId != null && targetId >= 0 && value != null) {
                    totalVotes += value;
                }
            }
            int majorityThreshold = Math.max(1, (totalVotes + 1) / 2);

            Long leadingId = null;
            int leadingVotes = 0;
            boolean tie = false;

            for (java.util.Map.Entry<Long, Integer> entry : counts.entrySet()) {
                Long targetId = entry.getKey();
                if (targetId == null || targetId < 0) {
                    continue;
                }
                int votes = entry.getValue();
                if (votes > leadingVotes) {
                    leadingVotes = votes;
                    leadingId = targetId;
                    tie = false;
                } else if (votes == leadingVotes && votes > 0 && !java.util.Objects.equals(leadingId, targetId)) {
                    tie = true;
                }
            }

            if (!tie && leadingId != null && leadingVotes >= majorityThreshold) {
                Player expelledPlayer = null;
                for (Player candidate : match.getPlayers()) {
                    if (candidate.getId().equals(leadingId)) {
                        expelledPlayer = candidate;
                        break;
                    }
                }
                if (expelledPlayer != null) {
                    expelledPlayer.setAlive(false);
                    match.stopVoting();
                    match.getVotesByPlayer().clear();

                    VoteResult result;
                    if (expelledPlayer.isInfiltrator()) {
                        match.setWinnerMessage("¡El infiltrado ha sido identificado y eliminado! Los náufragos ganan");
                        match.endMatch();
                        gameEngine.stopMatchTicker(code);
                        result = new VoteResult(counts, leadingId, "human",
                                "El infiltrado fue expulsado por mayoría. Los náufragos ganan.");
                    } else {
                        result = new VoteResult(counts, leadingId, "human",
                                "Un jugador humano fue expulsado por mayoría.");
                    }

                    webSocketController.broadcastVoteResult(code, result);
                    webSocketController.broadcastGameState(code, buildGameStateForMatch(match));
                    return;
                }

                Npc expelledNpc = null;
                for (Npc npc : new ArrayList<>(match.getNpcs())) {
                    if (npc.getId().equals(leadingId)) {
                        expelledNpc = npc;
                        break;
                    }
                }
                if (expelledNpc != null) {
                    expelledNpc.deactivate();
                    match.getNpcs().remove(expelledNpc);
                    match.stopVoting();
                    match.getVotesByPlayer().clear();

                    boolean infiltratorNpcVictory = checkNpcOnlyInfiltratorLeft(match, code);

                    String resultMessage = infiltratorNpcVictory
                            ? "Se expulsó un NPC por mayoría. El infiltrado ha ganado, todos los demás NPC han sido eliminados."
                            : "Se expulsó un NPC por mayoría.";

                    VoteResult result = new VoteResult(counts, leadingId, "npc",
                            resultMessage);
                    webSocketController.broadcastVoteResult(code, result);
                    webSocketController.broadcastGameState(code, buildGameStateForMatch(match));
                    return;
                }
            }

            match.stopVoting();
            if (match.getVotesByPlayer() != null) {
                match.getVotesByPlayer().clear();
            }

            String message;
            if (counts.isEmpty()) {
                message = dueToTimeout ? "La votación terminó sin votos. Nadie fue expulsado."
                        : "Nadie votó. Nadie fue expulsado.";
            } else {
                message = dueToTimeout ? "La votación terminó sin mayoría. Nadie fue expulsado."
                        : "No hubo mayoría. Nadie fue expulsado.";
            }

            VoteResult result = new VoteResult(counts, null, "none", message);
            webSocketController.broadcastVoteResult(code, result);
            webSocketController.broadcastGameState(code, buildGameStateForMatch(match));
        }
    }

    @PostMapping("/{code}/eliminate")
    public ResponseEntity<?> eliminate(@PathVariable String code, @RequestBody VoteRequest req) {
        Match match = matchService.getMatchByCode(code);
        if (match == null)
            return ResponseEntity.badRequest().body("Partida no encontrada.");
        if (match.getStatus() == null || !match.getStatus().name().equals("STARTED"))
            return ResponseEntity.badRequest().body("La partida no está en curso.");
        if (req.getUsername() == null || req.getTargetId() == null)
            return ResponseEntity.badRequest().body("Solicitud inválida.");

        Player killer = match.getPlayers().stream()
                .filter(pl -> pl.getUsername().equals(req.getUsername()))
                .findFirst().orElse(null);
        if (killer == null || !killer.isAlive())
            return ResponseEntity.status(403).body("Asesino no válido.");
        if (!killer.isInfiltrator())
            return ResponseEntity.status(403).body("Solo el infiltrado puede eliminar.");

        Player target = match.getPlayers().stream()
                .filter(pl -> pl.getId().equals(req.getTargetId()))
                .findFirst().orElse(null);
        if (target == null || !target.isAlive() || target.isInfiltrator())
            return ResponseEntity.status(403).body("Objetivo inválido.");

        Position killerPos = killer.getPosition();
        Position targetPos = target.getPosition();
        double dist = distance(killerPos, targetPos);
        if (dist > ELIMINATION_RANGE)
            return ResponseEntity.status(403).body("Fuera de rango para eliminar.");

        synchronized (match) {
            if (!target.isAlive())
                return ResponseEntity.status(409).body("El objetivo ya está eliminado.");
            target.setAlive(false);

            // <-- comprobar si quedan náufragos vivos
            boolean anyHumanAlive = match.getPlayers().stream()
                    .anyMatch(p -> !p.isInfiltrator() && p.isAlive());
            if (!anyHumanAlive) {
                // el infiltrado eliminó a todos los náufragos -> gana el infiltrado
                match.setWinnerMessage("¡El infiltrado ha ganado eliminando a todos los náufragos!");
                match.endMatch();
                gameEngine.stopMatchTicker(code);
            }
        }

        EliminationEvent evt = new EliminationEvent(target.getId(), target.getUsername(), "Has sido eliminado.");
        webSocketController.broadcastElimination(code, evt);
        // broadcast game state que incluirá el winnerMessage si se finalizó la partida
        webSocketController.broadcastGameState(code, buildGameStateForMatch(match));

        return ResponseEntity.ok("Eliminación aplicada");
    }

    @PostMapping("/{code}/fuel")
    public ResponseEntity<?> modifyFuel(@PathVariable String code, @RequestBody FuelActionRequest req) {
        Match match = matchService.getMatchByCode(code);
        if (match == null)
            return ResponseEntity.badRequest().body("Partida no encontrada.");
        if (match.getStatus() == null || !match.getStatus().name().equals("STARTED"))
            return ResponseEntity.badRequest().body("La partida no está en curso.");
        if (req.getUsername() == null || req.getAction() == null)
            return ResponseEntity.badRequest().body("Solicitud inválida.");

        Player actor = match.getPlayers().stream()
                .filter(pl -> pl.getUsername().equals(req.getUsername()))
                .findFirst().orElse(null);
        if (actor == null || !actor.isAlive())
            return ResponseEntity.status(403).body("Jugador no válido.");

        double boatDist = computeDistanceToBoat(actor.getPosition());
        boolean requiresProximity = !actor.isInfiltrator();
        if (requiresProximity && boatDist > BOAT_INTERACTION_RADIUS)
            return ResponseEntity.status(403).body("Debes acercarte al barco.");

        if (!match.isFuelWindowOpenNow()) {
            int seconds = match.getFuelWindowSecondsRemaining();
            String msg = "Tanque de gasolina bloqueado temporalmente.";
            if (seconds > 0) {
                msg += " Disponible en " + seconds + "s.";
            }
            return ResponseEntity.status(423).body(msg);
        }

        double step = req.getAmount() != null ? req.getAmount() : FUEL_STEP;
        double delta;
        switch (req.getAction()) {
            case FILL:
                if (actor.isInfiltrator())
                    return ResponseEntity.status(403).body("El infiltrado no puede llenar el tanque.");
                delta = Math.abs(step);
                break;
            case SABOTAGE:
                if (!actor.isInfiltrator())
                    return ResponseEntity.status(403).body("Solo el infiltrado puede sabotear.");
                delta = -Math.abs(step);
                break;
            default:
                return ResponseEntity.badRequest().body("Acción desconocida.");
        }

        double updated;
        boolean completed;
        synchronized (match) {
            if (match.getStatus() == MatchStatus.FINISHED)
                return ResponseEntity.status(409).body("La partida ya terminó.");
            double before = match.getFuelPercentage();
            updated = match.adjustFuel(delta);
            completed = updated >= 100.0 && before < 100.0;
            if (completed) {
                // <-- fijar el winnerMessage antes de finalizar
                match.setWinnerMessage("¡El barco ha sido reparado a tiempo! Los náufragos escapan con éxito");
                match.endMatch();
                gameEngine.stopMatchTicker(code);
            }
        }

        GameState state = buildGameStateForMatch(match);

        // broadcast final state (incluye winnerMessage)
        webSocketController.broadcastGameState(code, state);

        return ResponseEntity.ok(new FuelActionResponse(updated, match.getStatus() != null
                ? match.getStatus().name().toLowerCase(Locale.ROOT)
                : "unknown"));
    }

    private double computeDistanceToBoat(Position position) {
        if (position == null)
            return Double.MAX_VALUE;
        return Math.hypot(position.getX() - BOAT_X, position.getY() - BOAT_Y);
    }

    private boolean checkNpcOnlyInfiltratorLeft(Match match, String code) {
        Player infiltrator = match.getInfiltrator();
        boolean infiltratorAlive = infiltrator != null && infiltrator.isAlive();
        if (!infiltratorAlive) {
            return false;
        }

        boolean anyActiveNpc = false;
        for (Npc npc : match.getNpcs()) {
            if (npc.isActive()) {
                anyActiveNpc = true;
                break;
            }
        }

        if (!anyActiveNpc) {
            match.setWinnerMessage("El infiltrado ha ganado, todos los demás NPC han sido eliminados");
            match.endMatch();
            gameEngine.stopMatchTicker(code);
            return true;
        }

        return false;
    }

    private double distance(Position a, Position b) {
        double ax = a != null ? a.getX() : 0.0;
        double ay = a != null ? a.getY() : 0.0;
        double bx = b != null ? b.getX() : 0.0;
        double by = b != null ? b.getY() : 0.0;
        return Math.hypot(ax - bx, ay - by);
    }

}
