package com.arsw.shipwreckeds.controller;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.MatchStatus;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.Position;
import org.springframework.http.HttpStatus;
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
import java.util.Map;

/**
 * Controlador que maneja creación y unión a partidas (lobbies).
 * Implementación limpia y con buenas prácticas de imports y estructura.
 */
@RestController
@RequestMapping("/api/match")
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
    public ResponseEntity<Object> createMatch(@RequestBody CreateMatchRequest req) {
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
    public ResponseEntity<Object> joinMatch(@RequestBody JoinMatchRequest req) {
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
    public ResponseEntity<Object> startMatch(@PathVariable String code, @RequestParam String hostName) {
        try {
            Match match = matchService.updateMatch(code, current -> {
                if (current.getPlayers().isEmpty() || !current.getPlayers().get(0).getUsername().equals(hostName)) {
                    throw new MatchOperationException(HttpStatus.FORBIDDEN,
                            "Solo el host puede iniciar la partida.");
                }

                if (current.getPlayers().size() < 5) {
                    throw new IllegalArgumentException(
                            "No hay suficientes jugadores para iniciar la partida. Se requieren 5 jugadores humanos.");
                }

                roleService.assignHumanRoles(current);
                npcService.generateNpcs(current);
                current.startMatch();

                double islandRadius = 100.0;
                java.util.Random rnd = new java.util.Random();
                for (Player p : current.getPlayers()) {
                    if (p.getPosition() == null) {
                        double ang = rnd.nextDouble() * Math.PI * 2;
                        double r = rnd.nextDouble() * (islandRadius * 0.7);
                        double x = Math.cos(ang) * r;
                        double y = Math.sin(ang) * r;
                        p.setPosition(new Position(x, y));
                    }
                }
                for (Npc n : current.getNpcs()) {
                    if (n.getPosition() == null) {
                        double ang = rnd.nextDouble() * Math.PI * 2;
                        double r = rnd.nextDouble() * (islandRadius * 0.7);
                        double x = Math.cos(ang) * r;
                        double y = Math.sin(ang) * r;
                        n.setPosition(new Position(x, y));
                        current.resetFuel();
                    }
                }
                return current;
            });

            webSocketController.broadcastLobbyUpdate(match);
            webSocketController.broadcastGameState(match.getCode(), buildGameStateForMatch(match));
            gameEngine.startMatchTicker(match.getCode());
            return ResponseEntity.ok(match);
        } catch (MatchOperationException moe) {
            return ResponseEntity.status(moe.getStatus()).body(moe.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{code}/startVote")
    public ResponseEntity<Object> startVote(@PathVariable String code, @RequestParam String username) {
        try {
            VoteStartContext ctx = matchService.updateMatch(code, match -> {
                if (match.getStatus() == null || !match.getStatus().name().equals("STARTED")) {
                    throw new IllegalArgumentException("La partida no está en curso.");
                }

                Player p = match.getPlayers().stream().filter(pl -> pl.getUsername().equals(username)).findFirst()
                        .orElse(null);
                if (p == null || !p.isAlive()) {
                    throw new MatchOperationException(HttpStatus.FORBIDDEN,
                            "Jugador no válido para iniciar votación.");
                }
                if (p.isInfiltrator()) {
                    throw new MatchOperationException(HttpStatus.FORBIDDEN,
                            "El infiltrado no puede iniciar votaciones.");
                }

                if (match.isVotingActive()) {
                    throw new IllegalArgumentException("Ya hay una votación en curso.");
                }

                match.startVoting();
                long voteEndsAt = match.getVoteStartEpochMs() + Match.VOTE_DURATION_SECONDS * 1000L;
                GameState state = buildGameStateForMatch(match);
                VoteStart vs = new VoteStart(buildVoteOptions(match),
                        "Iniciar votación: elige un NPC para expulsar",
                        Match.VOTE_DURATION_SECONDS,
                        voteEndsAt);
                return new VoteStartContext(state, vs);
            });

            gameEngine.scheduleVoteTimeout(code, () -> concludeVote(code, true));
            webSocketController.broadcastGameState(code, ctx.gameState());
            webSocketController.broadcastVoteStart(code, ctx.voteStart());
            return ResponseEntity.ok("Votación iniciada");
        } catch (MatchOperationException moe) {
            return ResponseEntity.status(moe.getStatus()).body(moe.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{code}/vote")
    public ResponseEntity<Object> submitVote(@PathVariable String code, @RequestBody VoteRequest req) {
        try {
            VoteSubmissionResult result = matchService.updateMatch(code, match -> {
                if (!match.isVotingActive()) {
                    throw new IllegalArgumentException("No hay votación activa.");
                }

                Player voter = match.getPlayers().stream().filter(pl -> pl.getUsername().equals(req.getUsername()))
                        .findFirst()
                        .orElse(null);
                if (voter == null || !voter.isAlive()) {
                    throw new MatchOperationException(HttpStatus.FORBIDDEN, "Jugador no válido para votar.");
                }
                if (voter.isInfiltrator()) {
                    throw new MatchOperationException(HttpStatus.FORBIDDEN, "El infiltrado no puede votar.");
                }

                match.recordVote(req.getUsername(), req.getTargetId());
                boolean shouldConclude = match.allHumansVoted();
                VoteAck ack = new VoteAck(req.getUsername(),
                        "Voto registrado correctamente, esperando resultados finales.");
                return new VoteSubmissionResult(ack, shouldConclude);
            });

            if (result.shouldConclude()) {
                concludeVote(code, false);
            }

            return ResponseEntity.ok(result.ack());
        } catch (MatchOperationException moe) {
            return ResponseEntity.status(moe.getStatus()).body(moe.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{code}")
    public ResponseEntity<Object> getMatch(@PathVariable String code) {
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
        boolean votingActive = match.isVotingActive();
        long voteEndsAt = votingActive ? match.getVoteStartEpochMs() + Match.VOTE_DURATION_SECONDS * 1000L : 0L;
        List<AvatarState> voteOptions = votingActive ? buildVoteOptions(match) : null;
        VoteResult lastVoteResult = match.getLastVoteResult();
        long lastVoteResultEpoch = match.getLastVoteResultEpochMs();
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
                match.getFuelWindowSecondsRemaining(),
                votingActive,
                voteEndsAt,
                voteOptions,
                lastVoteResult,
                lastVoteResultEpoch);

    }

    private List<AvatarState> buildVoteOptions(Match match) {
        List<AvatarState> options = new ArrayList<>();
        for (Npc n : match.getNpcs()) {
            Position pos = n.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            options.add(new AvatarState(n.getId(), "npc", null, x, y, n.isInfiltrator(), n.isActive(),
                    n.getDisplayName()));
        }
        for (Player pl : match.getPlayers()) {
            if (pl.isInfiltrator() && pl.isAlive()) {
                Position pos = pl.getPosition();
                double x = pos != null ? pos.getX() : 0.0;
                double y = pos != null ? pos.getY() : 0.0;
                options.add(new AvatarState(pl.getId(), "npc", null, x, y, true, pl.isAlive(),
                        GameEngine.buildNpcAlias(pl.getId())));
            }
        }
        return options;
    }

    private void concludeVote(String code, boolean dueToTimeout) {
        VoteResultContext ctx;
        try {
            ctx = matchService.updateMatch(code, match -> {
                if (!match.isVotingActive()) {
                    return null;
                }

                gameEngine.cancelVoteTimeout(code);

                java.util.Map<Long, Integer> counts = new java.util.LinkedHashMap<>();
                int abstentions = 0;
                if (match.getVotesByPlayer() != null) {
                    for (Long tid : match.getVotesByPlayer().values()) {
                        if (tid == null) {
                            continue;
                        }
                        if (tid < 0) {
                            abstentions++;
                            continue;
                        }
                        counts.put(tid, counts.getOrDefault(tid, 0) + 1);
                    }
                }

                int totalVotes = counts.entrySet().stream()
                        .filter(entry -> entry.getKey() != null && entry.getKey() >= 0 && entry.getValue() != null)
                        .mapToInt(Map.Entry::getValue)
                        .sum();
                int majorityThreshold = totalVotes > 0
                        ? (int) Math.ceil(totalVotes / 2.0)
                        : 1;

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
                    final Long candidateId = leadingId;
                    Player expelledPlayer = match.getPlayers().stream()
                            .filter(candidate -> candidate.getId().equals(candidateId))
                            .findFirst()
                            .orElse(null);
                    if (expelledPlayer != null) {
                        expelledPlayer.setAlive(false);
                        match.stopVoting();
                        match.getVotesByPlayer().clear();

                        VoteResult result;
                        if (expelledPlayer.isInfiltrator()) {
                            match.setWinnerMessage(
                                    "¡El infiltrado ha sido identificado y eliminado! Los náufragos ganan");
                            match.endMatch();
                            gameEngine.stopMatchTicker(code);
                            result = new VoteResult(counts, leadingId, "human",
                                    "El infiltrado fue expulsado por mayoría. Los náufragos ganan.", abstentions, 0L);
                        } else {
                            result = new VoteResult(counts, leadingId, "human",
                                    "Un jugador humano fue expulsado por mayoría.", abstentions, 0L);
                        }

                        VoteResult stamped = stampAndStoreResult(match, result);
                        return new VoteResultContext(stamped, buildGameStateForMatch(match));
                    }

                    final Long npcTargetId = leadingId;
                    Npc expelledNpc = match.getNpcs().stream()
                            .filter(npc -> npc.getId().equals(npcTargetId))
                            .findFirst()
                            .orElse(null);
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
                                resultMessage, abstentions, 0L);
                        VoteResult stamped = stampAndStoreResult(match, result);
                        return new VoteResultContext(stamped, buildGameStateForMatch(match));
                    }
                }

                match.stopVoting();
                if (match.getVotesByPlayer() != null) {
                    match.getVotesByPlayer().clear();
                }

                String message;
                if (counts.isEmpty()) {
                    if (abstentions > 0) {
                        message = dueToTimeout
                                ? "La votación terminó sin mayoría (solo abstenciones). Nadie fue expulsado."
                                : "Todos se abstuvieron. Nadie fue expulsado.";
                    } else {
                        message = dueToTimeout ? "La votación terminó sin votos. Nadie fue expulsado."
                                : "Nadie votó. Nadie fue expulsado.";
                    }
                } else {
                    message = dueToTimeout ? "La votación terminó sin mayoría. Nadie fue expulsado."
                            : "No hubo mayoría. Nadie fue expulsado.";
                }

                VoteResult result = new VoteResult(counts, null, "none", message, abstentions, 0L);
                VoteResult stamped = stampAndStoreResult(match, result);
                return new VoteResultContext(stamped, buildGameStateForMatch(match));
            });
        } catch (IllegalArgumentException e) {
            return;
        }

        if (ctx == null) {
            return;
        }

        webSocketController.broadcastVoteResult(code, ctx.result());
        webSocketController.broadcastGameState(code, ctx.gameState());
    }

    private VoteResult stampAndStoreResult(Match match, VoteResult result) {
        long publishedAt = System.currentTimeMillis();
        result.setPublishedAtEpochMs(publishedAt);
        match.setLastVoteResult(result);
        match.setLastVoteResultEpochMs(publishedAt);
        return result;
    }

    @PostMapping("/{code}/eliminate")
    public ResponseEntity<Object> eliminate(@PathVariable String code, @RequestBody VoteRequest req) {
        if (req.getUsername() == null || req.getTargetId() == null)
            return ResponseEntity.badRequest().body("Solicitud inválida.");

        try {
            EliminationContext ctx = matchService.updateMatch(code, match -> {
                if (match.getStatus() == null || !match.getStatus().name().equals("STARTED")) {
                    throw new IllegalArgumentException("La partida no está en curso.");
                }

                Player killer = match.getPlayers().stream()
                        .filter(pl -> pl.getUsername().equals(req.getUsername()))
                        .findFirst().orElse(null);
                if (killer == null || !killer.isAlive()) {
                    throw new MatchOperationException(HttpStatus.FORBIDDEN, "Asesino no válido.");
                }
                if (!killer.isInfiltrator()) {
                    throw new MatchOperationException(HttpStatus.FORBIDDEN, "Solo el infiltrado puede eliminar.");
                }

                Player target = match.getPlayers().stream()
                        .filter(pl -> pl.getId().equals(req.getTargetId()))
                        .findFirst().orElse(null);
                if (target == null || !target.isAlive() || target.isInfiltrator()) {
                    throw new MatchOperationException(HttpStatus.FORBIDDEN, "Objetivo inválido.");
                }

                Position killerPos = killer.getPosition();
                Position targetPos = target.getPosition();
                double dist = distance(killerPos, targetPos);
                if (dist > ELIMINATION_RANGE) {
                    throw new MatchOperationException(HttpStatus.FORBIDDEN, "Fuera de rango para eliminar.");
                }

                if (!target.isAlive()) {
                    throw new MatchOperationException(HttpStatus.CONFLICT, "El objetivo ya está eliminado.");
                }
                target.setAlive(false);

                boolean anyHumanAlive = match.getPlayers().stream()
                        .anyMatch(p -> !p.isInfiltrator() && p.isAlive());
                boolean matchFinished = false;
                if (!anyHumanAlive) {
                    match.setWinnerMessage("¡El infiltrado ha ganado eliminando a todos los náufragos!");
                    match.endMatch();
                    matchFinished = true;
                }

                EliminationEvent evt = new EliminationEvent(target.getId(), target.getUsername(),
                        "Has sido eliminado.");
                return new EliminationContext(evt, buildGameStateForMatch(match), matchFinished);
            });

            if (ctx.matchFinished()) {
                gameEngine.stopMatchTicker(code);
            }
            webSocketController.broadcastElimination(code, ctx.event());
            webSocketController.broadcastGameState(code, ctx.gameState());
            return ResponseEntity.ok("Eliminación aplicada");
        } catch (MatchOperationException moe) {
            return ResponseEntity.status(moe.getStatus()).body(moe.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{code}/fuel")
    public ResponseEntity<Object> modifyFuel(@PathVariable String code, @RequestBody FuelActionRequest req) {
        if (req.getUsername() == null || req.getAction() == null)
            return ResponseEntity.badRequest().body("Solicitud inválida.");

        try {
            FuelActionContext ctx = matchService.updateMatch(code, match -> {
                if (match.getStatus() == null || !match.getStatus().name().equals("STARTED")) {
                    throw new IllegalArgumentException("La partida no está en curso.");
                }

                Player actor = match.getPlayers().stream()
                        .filter(pl -> pl.getUsername().equals(req.getUsername()))
                        .findFirst().orElse(null);
                if (actor == null || !actor.isAlive()) {
                    throw new MatchOperationException(HttpStatus.FORBIDDEN, "Jugador no válido.");
                }

                double boatDist = computeDistanceToBoat(actor.getPosition());
                boolean requiresProximity = !actor.isInfiltrator();
                if (requiresProximity && boatDist > BOAT_INTERACTION_RADIUS) {
                    throw new MatchOperationException(HttpStatus.FORBIDDEN, "Debes acercarte al barco.");
                }

                if (!match.isFuelWindowOpenNow()) {
                    int seconds = match.getFuelWindowSecondsRemaining();
                    String msg = "Tanque de gasolina bloqueado temporalmente.";
                    if (seconds > 0) {
                        msg += " Disponible en " + seconds + "s.";
                    }
                    throw new MatchOperationException(HttpStatus.LOCKED, msg);
                }

                double step = req.getAmount() != null ? req.getAmount() : FUEL_STEP;
                double delta;
                switch (req.getAction()) {
                    case FILL:
                        if (actor.isInfiltrator()) {
                            throw new MatchOperationException(HttpStatus.FORBIDDEN,
                                    "El infiltrado no puede llenar el tanque.");
                        }
                        delta = Math.abs(step);
                        break;
                    case SABOTAGE:
                        if (!actor.isInfiltrator()) {
                            throw new MatchOperationException(HttpStatus.FORBIDDEN,
                                    "Solo el infiltrado puede sabotear.");
                        }
                        delta = -Math.abs(step);
                        break;
                    default:
                        throw new IllegalArgumentException("Acción desconocida.");
                }

                if (match.getStatus() == MatchStatus.FINISHED) {
                    throw new MatchOperationException(HttpStatus.CONFLICT, "La partida ya terminó.");
                }
                double before = match.getFuelPercentage();
                double updated = match.adjustFuel(delta);
                boolean completed = updated >= 100.0 && before < 100.0;
                if (completed) {
                    match.setWinnerMessage("¡El barco ha sido reparado a tiempo! Los náufragos escapan con éxito");
                    match.endMatch();
                }

                FuelActionResponse response = new FuelActionResponse(updated, match.getStatus() != null
                        ? match.getStatus().name().toLowerCase(Locale.ROOT)
                        : "unknown");

                return new FuelActionContext(response, buildGameStateForMatch(match), completed);
            });

            if (ctx.matchCompleted()) {
                gameEngine.stopMatchTicker(code);
            }

            webSocketController.broadcastGameState(code, ctx.gameState());
            return ResponseEntity.ok(ctx.response());
        } catch (MatchOperationException moe) {
            return ResponseEntity.status(moe.getStatus()).body(moe.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
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

    private record VoteStartContext(GameState gameState, VoteStart voteStart) {
    }

    private record VoteSubmissionResult(VoteAck ack, boolean shouldConclude) {
    }

    private record VoteResultContext(VoteResult result, GameState gameState) {
    }

    private record EliminationContext(EliminationEvent event, GameState gameState, boolean matchFinished) {
    }

    private record FuelActionContext(FuelActionResponse response, GameState gameState, boolean matchCompleted) {
    }

}
