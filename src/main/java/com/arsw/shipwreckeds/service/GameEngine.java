package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.controller.WebSocketController;
import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.Position;
import com.arsw.shipwreckeds.model.dto.AvatarState;
import com.arsw.shipwreckeds.model.dto.GameState;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class GameEngine {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "game-engine");
        t.setDaemon(true);
        return t;
    });

    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
    private final WebSocketController ws;

    public GameEngine(WebSocketController ws) {
        this.ws = ws;
    }

    public void startMatchTicker(Match match) {
        if (match == null)
            return;
        String code = match.getCode();
        stopMatchTicker(code);

        Runnable tick = () -> {
            synchronized (match) {
                if (match.getStatus() != null && match.getStatus().name().equals("STARTED")) {
                    int t = match.getTimerSeconds();
                    if (t <= 0) {
                        match.endMatch();
                        // broadcast final state
                        ws.broadcastGameState(code, buildGameState(match));
                        stopMatchTicker(code);
                        return;
                    }
                    match.setTimerSeconds(t - 1);
                    // broadcast updated GameState with new timer
                    ws.broadcastGameState(code, buildGameState(match));
                } else {
                    // if match not started, cancel
                    stopMatchTicker(code);
                }
            }
        };

        ScheduledFuture<?> f = scheduler.scheduleAtFixedRate(tick, 1, 1, TimeUnit.SECONDS);
        tasks.put(code, f);
    }

    public void stopMatchTicker(String code) {
        ScheduledFuture<?> f = tasks.remove(code);
        if (f != null)
            f.cancel(false);
    }

    private GameState buildGameState(Match match) {
        List<AvatarState> avatars = new ArrayList<>();
        for (Player p : match.getPlayers()) {
            Position pos = p.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            avatars.add(new AvatarState(p.getId(), "human", p.getUsername(), x, y, p.isInfiltrator(), p.isAlive()));
        }
        for (Npc n : match.getNpcs()) {
            Position pos = n.getPosition();
            double x = pos != null ? pos.getX() : 0.0;
            double y = pos != null ? pos.getY() : 0.0;
            avatars.add(new AvatarState(n.getId(), "npc", null, x, y, false, true));
        }
        GameState.Island isl = new GameState.Island(0.0, 0.0, 100.0);
        return new GameState(match.getCode(), System.currentTimeMillis(), match.getTimerSeconds(), isl, avatars);
    }

    @PreDestroy
    public void shutdown() {
        for (ScheduledFuture<?> f : tasks.values())
            f.cancel(false);
        scheduler.shutdownNow();
    }
}
