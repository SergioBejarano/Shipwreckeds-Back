package com.arsw.shipwreckeds.controller;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.MatchStatus;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.Position;
import com.arsw.shipwreckeds.model.dto.AvatarState;
import com.arsw.shipwreckeds.model.dto.GameState;
import com.arsw.shipwreckeds.model.dto.MoveCommand;
import com.arsw.shipwreckeds.service.AuthService;
import com.arsw.shipwreckeds.service.GameEngine;
import com.arsw.shipwreckeds.service.MatchService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * STOMP message controller that validates and applies movement commands before
 * broadcasting updated game state snapshots.
 */
@Controller
public class GameController {

    private static final double ISLAND_RADIUS = 100.0;
    private static final double BOAT_X = ISLAND_RADIUS + 12.0;
    private static final double BOAT_Y = 0.0;
    private static final double BOAT_INTERACTION_RADIUS = 40.0;

    private final MatchService matchService;
    private final AuthService authService;
    private final WebSocketController webSocketController;

    private final Map<Long, Long> lastMoveTsByAvatar = new ConcurrentHashMap<>();

    /**
     * Creates a controller that handles player movement synchronization.
     *
     * @param matchService        service used to retrieve active matches
     * @param authService         service that validates player sessions
     * @param webSocketController broadcaster for updated game state messages
     */
    public GameController(MatchService matchService, AuthService authService, WebSocketController webSocketController) {
        this.matchService = matchService;
        this.authService = authService;
        this.webSocketController = webSocketController;
    }

    /**
     * Processes movement commands sent by clients while enforcing ownership and
     * rate limiting rules.
     *
     * @param code match identifier extracted from the STOMP destination
     * @param cmd  payload with avatar id, direction and issuing player
     */
    @MessageMapping("/game/{code}/move")
    public void handleMove(@DestinationVariable String code, MoveCommand cmd) {
        if (cmd == null || cmd.getUsername() == null || cmd.getAvatarId() == null || cmd.getDirection() == null)
            return;

        // validate session
        Player session = authService.getPlayer(cmd.getUsername());
        if (session == null)
            return;
        try {
            GameState updatedState = matchService.updateMatch(code, match -> {
                if (match.getStatus() == null || !match.getStatus().name().equals("STARTED")) {
                    return null;
                }

                Player target = match.getPlayers().stream()
                        .filter(p -> p.getId().equals(cmd.getAvatarId()))
                        .findFirst().orElse(null);
                if (target == null) {
                    return null;
                }

                if (!target.getUsername().equals(cmd.getUsername())) {
                    return null;
                }

                long now = System.currentTimeMillis();
                Long last = lastMoveTsByAvatar.getOrDefault(target.getId(), 0L);
                if (now - last < 100) {
                    return null;
                }

                double dx = cmd.getDirection().getDx();
                double dy = cmd.getDirection().getDy();
                double len = Math.hypot(dx, dy);
                if (Double.isNaN(len) || len < 1e-6)
                    return null;
                dx /= len;
                dy /= len;

                double step = ISLAND_RADIUS * 0.035;

                Position pos = target.getPosition();
                if (pos == null) {
                    pos = new Position(0.0, 0.0);
                    target.setPosition(pos);
                }
                double proposedX = pos.getX() + dx * step;
                double proposedY = pos.getY() + dy * step;
                double distToCenter = Math.hypot(proposedX, proposedY);
                double margin = 0.5;
                if (distToCenter > ISLAND_RADIUS - margin) {
                    double vecX = proposedX;
                    double vecY = proposedY;
                    double plen = Math.hypot(vecX, vecY);
                    if (plen == 0)
                        plen = 1;
                    double clampedX = (vecX / plen) * (ISLAND_RADIUS - margin);
                    double clampedY = (vecY / plen) * (ISLAND_RADIUS - margin);
                    target.moveTo(new Position(clampedX, clampedY));
                } else {
                    target.moveTo(new Position(proposedX, proposedY));
                }

                lastMoveTsByAvatar.put(target.getId(), now);
                return buildGameState(match);
            });

            if (updatedState != null) {
                webSocketController.broadcastGameState(code, updatedState);
            }
        } catch (IllegalArgumentException ignored) {
            // No-op: match no longer exists or not ready
        }
    }

    /**
     * Builds a {@link GameState} snapshot representing the current state of the
     * requested match.
     *
     * @param match match whose state needs to be broadcast
     * @return immutable snapshot ready to send over WebSocket
     */
    private GameState buildGameState(Match match) {
        List<AvatarState> avatars = new ArrayList<>();
        for (Player p : match.getPlayers()) {
            Position pos = p.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;

            if (p.isInfiltrator()) {
                String dname = GameEngine.buildNpcAlias(p.getId());
                avatars.add(new AvatarState(p.getId(), "npc", null, x, y, true, p.isAlive(), dname));
            } else {
                avatars.add(new AvatarState(p.getId(), "human", p.getUsername(), x, y, false, p.isAlive(),
                        p.getUsername()));
            }
        }

        for (Npc n : match.getNpcs()) {
            Position pos = n.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            avatars.add(new AvatarState(n.getId(), "npc", null, x, y, n.isInfiltrator(), n.isActive(),
                    n.getDisplayName()));
        }

        GameState.Island isl = new GameState.Island(0.0, 0.0, ISLAND_RADIUS);
        GameState.Boat boat = new GameState.Boat(BOAT_X, BOAT_Y, BOAT_INTERACTION_RADIUS);
        String status = match.getStatus() != null ? match.getStatus().name() : MatchStatus.WAITING.name();

        // Winner message propagated to the frontend when available
        String winnerMessage = match.getWinnerMessage();

        boolean votingActive = match.isVotingActive();
        long voteDeadline = votingActive ? match.getVoteStartEpochMs() + Match.VOTE_DURATION_SECONDS * 1000L : 0L;
        List<AvatarState> voteOptions = votingActive ? buildVoteOptions(match) : null;
        return new GameState(
                match.getCode(),
                System.currentTimeMillis(),
                match.getTimerSeconds(),
                isl,
                avatars,
                match.getFuelPercentage(),
                status,
                boat,
                winnerMessage,
                match.isFuelWindowOpenNow(),
                match.getFuelWindowSecondsRemaining(),
                votingActive,
                voteDeadline,
                voteOptions,
                match.getLastVoteResult(),
                match.getLastVoteResultEpochMs());
    }

    private List<AvatarState> buildVoteOptions(Match match) {
        List<AvatarState> options = new ArrayList<>();
        for (Npc npc : match.getNpcs()) {
            Position pos = npc.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            options.add(new AvatarState(npc.getId(), "npc", null, x, y, npc.isInfiltrator(), npc.isActive(),
                    npc.getDisplayName()));
        }
        for (Player p : match.getPlayers()) {
            if (p.isInfiltrator() && p.isAlive()) {
                Position pos = p.getPosition();
                double x = pos != null ? pos.getX() : 0.0;
                double y = pos != null ? pos.getY() : 0.0;
                options.add(new AvatarState(p.getId(), "npc", null, x, y, true, p.isAlive(),
                        GameEngine.buildNpcAlias(p.getId())));
            }
        }
        return options;
    }

}
