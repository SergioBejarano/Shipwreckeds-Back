package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.controller.WebSocketController;
import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.MatchStatus;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.Position;
import com.arsw.shipwreckeds.model.dto.AvatarState;
import com.arsw.shipwreckeds.model.dto.GameState;
import com.arsw.shipwreckeds.service.MatchService;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Server-side engine responsible for ticking matches, moving NPCs and
 * scheduling vote timers.
 */
@Service
public class GameEngine {

    private static final double ISLAND_RADIUS = 100.0;
    private static final double BOAT_X = ISLAND_RADIUS + 12.0;
    private static final double BOAT_Y = 0.0;
    private static final double BOAT_INTERACTION_RADIUS = 40.0;
    private static final long NPC_ALIAS_OFFSET = 100000L;
    private static final double NPC_SPEED_MULTIPLIER = 3.0;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "game-engine");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final Map<String, ScheduledFuture<?>> voteTimers = new ConcurrentHashMap<>();
    private final Map<String, Map<Long, Position>> npcTargetsByMatch = new ConcurrentHashMap<>();
    private final WebSocketController ws;
    private final MatchService matchService;

    public GameEngine(WebSocketController ws, MatchService matchService) {
        this.ws = ws;
        this.matchService = matchService;
    }

    /**
     * Starts the periodic task that updates a match timer and NPC movement.
     * Any previous ticker for the same match will be stopped.
     *
     * @param match match to tick
     */
    public void startMatchTicker(String code) {
        if (code == null || code.isBlank())
            return;
        stopMatchTicker(code);
        npcTargetsByMatch.remove(code);

        Runnable tick = () -> {
            TickResult result;
            try {
                result = matchService.updateMatch(code, current -> {
                    if (current.getStatus() == null || !current.getStatus().name().equals("STARTED")) {
                        return TickResult.stop(null);
                    }
                    int t = current.getTimerSeconds();
                    if (t <= 0) {
                        if (current.getWinnerMessage() == null || current.getWinnerMessage().isBlank()) {
                            current.setWinnerMessage("Se acabó el tiempo, ganó el infiltrado.");
                        }
                        current.endMatch();
                        return TickResult.stop(buildGameState(current));
                    }
                    updateNpcMovement(current, 1.0);
                    current.setTimerSeconds(t - 1);
                    return TickResult.keepRunning(buildGameState(current));
                });
            } catch (IllegalArgumentException ex) {
                // match disappeared
                stopMatchTicker(code);
                return;
            }

            if (result == null) {
                stopMatchTicker(code);
                return;
            }

            if (result.gameState != null) {
                ws.broadcastGameState(code, result.gameState);
            }

            if (result.stop) {
                stopMatchTicker(code);
            }
        };

        ScheduledFuture<?> f = scheduler.scheduleAtFixedRate(tick, 1, 1, TimeUnit.SECONDS);
        tasks.put(code, f);
    }

    /**
     * Stops the ticking task for the given match code, if any.
     *
     * @param code identifier of the match to stop
     */
    public void stopMatchTicker(String code) {
        ScheduledFuture<?> f = tasks.remove(code);
        if (f != null)
            f.cancel(false);
        npcTargetsByMatch.remove(code);
    }

    /**
     * Schedules the vote timeout task for a match, canceling any existing timer.
     *
     * @param match    match owning the timeout
     * @param callback logic to execute when the timer expires
     */
    public void scheduleVoteTimeout(String code, Runnable callback) {
        if (code == null || code.isBlank()) {
            return;
        }
        cancelVoteTimeout(code);
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            callback.run();
        }, Match.VOTE_DURATION_SECONDS, TimeUnit.SECONDS);
        voteTimers.put(code, future);
    }

    /**
     * Cancels the scheduled vote timeout for the specified match.
     *
     * @param code match identifier
     */
    public void cancelVoteTimeout(String code) {
        ScheduledFuture<?> future = voteTimers.remove(code);
        if (future != null) {
            future.cancel(false);
        }
    }

    /**
     * Creates a {@link GameState} snapshot for broadcasting.
     *
     * @param match match whose state is being serialized
     * @return ready-to-send game state payload
     */
    private GameState buildGameState(Match match) {
        List<AvatarState> avatars = new ArrayList<>();
        for (Player p : match.getPlayers()) {
            Position pos = p.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            if (p.isInfiltrator()) {
                avatars.add(new AvatarState(p.getId(), "npc", null, x, y, true, p.isAlive(),
                        aliasForInfiltrator(p.getId())));
            } else {
                avatars.add(new AvatarState(p.getId(), "human", p.getUsername(), x, y, false, p.isAlive(),
                        p.getUsername()));
            }
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

    /**
     * Updates NPC positions towards their assigned targets, creating new targets
     * when needed.
     *
     * @param match        match containing the NPCs
     * @param deltaSeconds elapsed seconds since the last update
     */
    private void updateNpcMovement(Match match, double deltaSeconds) {
        Map<Long, Position> targets = npcTargetsByMatch.computeIfAbsent(match.getCode(),
                k -> new ConcurrentHashMap<>());
        for (Npc npc : match.getNpcs()) {
            if (!npc.isActive()) {
                targets.remove(npc.getId());
                continue;
            }
            Position position = npc.getPosition();
            if (position == null) {
                position = new Position(0.0, 0.0);
                npc.setPosition(position);
            }

            Position target = targets.computeIfAbsent(npc.getId(), id -> randomIslandPoint());
            double dx = target.getX() - position.getX();
            double dy = target.getY() - position.getY();
            double distance = Math.hypot(dx, dy);

            if (distance < 1.0) {
                targets.put(npc.getId(), randomIslandPoint());
                continue;
            }

            double baseSpeed = Math.min(Math.max(0.15, npc.getMovementSpeed()), 0.55);
            double step = Math.min(distance, baseSpeed * NPC_SPEED_MULTIPLIER * deltaSeconds);
            double nx = position.getX() + (dx / distance) * step;
            double ny = position.getY() + (dy / distance) * step;

            double distFromCenter = Math.hypot(nx, ny);
            if (distFromCenter >= ISLAND_RADIUS - 2.0) {
                targets.put(npc.getId(), randomIslandPoint());
                continue;
            }

            position.setX(nx);
            position.setY(ny);
        }
    }

    /**
     * Generates a random position within the island radius.
     *
     * @return pseudo-random position constrained to the island
     */
    private Position randomIslandPoint() {
        double angle = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2);
        double radius = ThreadLocalRandom.current().nextDouble(0, ISLAND_RADIUS * 0.9);
        double x = Math.cos(angle) * radius;
        double y = Math.sin(angle) * radius;
        return new Position(x, y);
    }

    /**
     * Builds a stable alias to visually represent infiltrators as NPCs.
     *
     * @param baseId base id to incorporate into the alias
     * @return formatted alias string
     */
    public static String buildNpcAlias(long baseId) {
        return "NPC-" + (NPC_ALIAS_OFFSET + baseId);
    }

    /**
     * Resolves the alias used when rendering an infiltrator as an NPC.
     *
     * @param playerId infiltrator id
     * @return alias string used client-side
     */
    private String aliasForInfiltrator(Long playerId) {
        long id = playerId != null ? playerId : 0L;
        return buildNpcAlias(id);
    }

    /**
     * Cancels all scheduled tasks before the bean is destroyed.
     */
    @PreDestroy
    public void shutdown() {
        for (ScheduledFuture<?> f : tasks.values())
            f.cancel(false);
        for (ScheduledFuture<?> f : voteTimers.values())
            f.cancel(false);
        scheduler.shutdownNow();
    }

    private record TickResult(boolean stop, GameState gameState) {
        private static TickResult stop(GameState state) {
            return new TickResult(true, state);
        }

        private static TickResult keepRunning(GameState state) {
            return new TickResult(false, state);
        }
    }
}
