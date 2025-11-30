package com.arsw.shipwreckeds.controller;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.websocket.DistributedWsBroadcaster;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private final DistributedWsBroadcaster broadcaster;

    public WebSocketController(DistributedWsBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    /**
     * Envía el estado del lobby a todos los suscriptores en /topic/lobby/{code}
     */
    public void broadcastLobbyUpdate(Match match) {
        if (match == null || match.getCode() == null)
            return;
        String dest = "/topic/lobby/" + match.getCode();
        broadcaster.publish(dest, match);
    }

    /**
     * Publica el GameState completo a /topic/game/{code}
     */
    public void broadcastGameState(String code, Object gameState) {
        if (code == null)
            return;
        String dest = "/topic/game/" + code;
        broadcaster.publish(dest, gameState);
    }

    /**
     * Broadcast that a voting session has started. Payload can be a VoteStart DTO.
     */
    public void broadcastVoteStart(String code, Object voteStart) {
        if (code == null)
            return;
        String dest = "/topic/game/" + code + "/vote/start";
        broadcaster.publish(dest, voteStart);
    }

    /**
     * Broadcast final vote results.
     */
    public void broadcastVoteResult(String code, Object result) {
        if (code == null)
            return;
        String dest = "/topic/game/" + code + "/vote/result";
        broadcaster.publish(dest, result);
    }

    /**
     * Broadcast a player elimination event to /topic/game/{code}/elimination
     */
    public void broadcastElimination(String code, Object eliminationEvent) {
        if (code == null)
            return;
        String dest = "/topic/game/" + code + "/elimination";
        broadcaster.publish(dest, eliminationEvent);
    }
}
