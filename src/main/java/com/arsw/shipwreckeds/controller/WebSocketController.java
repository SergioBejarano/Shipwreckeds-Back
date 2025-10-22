package com.arsw.shipwreckeds.controller;

import com.arsw.shipwreckeds.model.Match;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Envía el estado del lobby a todos los suscriptores en /topic/lobby/{code}
     */
    public void broadcastLobbyUpdate(Match match) {
        if (match == null || match.getCode() == null)
            return;
        String dest = "/topic/lobby/" + match.getCode();
        messagingTemplate.convertAndSend(dest, match);
    }
}
