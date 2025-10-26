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
 * Controller para recibir comandos de movimiento vía STOMP y aplicar
 * lógica servidor-autoritativa mínima antes de reenviar el GameState.
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

    // simple rate-limit: last timestamp per avatar id (ms)
    private final Map<Long, Long> lastMoveTsByAvatar = new ConcurrentHashMap<>();

    public GameController(MatchService matchService, AuthService authService, WebSocketController webSocketController) {
        this.matchService = matchService;
        this.authService = authService;
        this.webSocketController = webSocketController;
    }

    @MessageMapping("/game/{code}/move")
    public void handleMove(@DestinationVariable String code, MoveCommand cmd) {
        if (cmd == null || cmd.getUsername() == null || cmd.getAvatarId() == null || cmd.getDirection() == null)
            return;

        Match match = matchService.getMatchByCode(code);
        if (match == null)
            return;
        if (match.getStatus() == null || !match.getStatus().name().equals("STARTED"))
            return;

        // validate session
        Player session = authService.getPlayer(cmd.getUsername());
        if (session == null)
            return;

        // find the player's avatar
        Player target = match.getPlayers().stream()
                .filter(p -> p.getId().equals(cmd.getAvatarId()))
                .findFirst().orElse(null);
        if (target == null)
            return; // not found or maybe NPC

        // ownership check
        if (!target.getUsername().equals(cmd.getUsername())) {
            // ignoring unauthorized movement attempt
            return;
        }

        // rate limit: 8 Hz -> 125 ms
        long now = System.currentTimeMillis();
        Long last = lastMoveTsByAvatar.getOrDefault(target.getId(), 0L);
        if (now - last < 100) { // allow slightly faster 10Hz tolerance
            return;
        }
        lastMoveTsByAvatar.put(target.getId(), now);

        // normalize direction
        double dx = cmd.getDirection().getDx();
        double dy = cmd.getDirection().getDy();
        double len = Math.hypot(dx, dy);
        if (Double.isNaN(len) || len < 1e-6)
            return;
        dx /= len;
        dy /= len;

        // step size
        double step = ISLAND_RADIUS * 0.035;

        synchronized (match) {
            Position pos = target.getPosition();
            if (pos == null) {
                pos = new Position(0.0, 0.0);
                target.setPosition(pos);
            }
            double proposedX = pos.getX() + dx * step;
            double proposedY = pos.getY() + dy * step;
            double distToCenter = Math.hypot(proposedX - 0.0, proposedY - 0.0);
            double margin = 0.5;
            if (distToCenter > ISLAND_RADIUS - margin) {
                double vecX = proposedX;
                double vecY = proposedY;
                double plen = Math.hypot(vecX, vecY);
                if (plen == 0)
                    plen = 1;
                double clampedX = (vecX / plen) * (ISLAND_RADIUS - margin);
                double clampedY = (vecY / plen) * (ISLAND_RADIUS - margin);
                // update
                target.moveTo(new Position(clampedX, clampedY));
            } else {
                target.moveTo(new Position(proposedX, proposedY));
            }

            // build GameState snapshot
            GameState gs = buildGameState(match);
            webSocketController.broadcastGameState(code, gs);
        }
    }

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
            AvatarState a = new AvatarState(n.getId(), "npc", null, x, y, false, true, n.getDisplayName());
            avatars.add(a);
        }
        GameState.Island isl = new GameState.Island(0.0, 0.0, ISLAND_RADIUS);
        GameState.Boat boat = new GameState.Boat(BOAT_X, BOAT_Y, BOAT_INTERACTION_RADIUS);
        String status = match.getStatus() != null ? match.getStatus().name() : MatchStatus.WAITING.name();
        return new GameState(match.getCode(), System.currentTimeMillis(), match.getTimerSeconds(), isl, avatars,
                match.getFuelPercentage(), status, boat);
    }
}
