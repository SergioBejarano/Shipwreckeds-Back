package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.controller.WebSocketController;
import com.arsw.shipwreckeds.model.Match;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;


/**
 * Pruebas unitarias para WebSocketController.
 *
 *
 * @author Daniel Ruge
 * @version 2025-10-29
 */
@ExtendWith(MockitoExtension.class)
class WebSocketControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketController webSocketController;

    @BeforeEach
    void setUp() {
        // inyectado por @InjectMocks
    }

    @Test
    void broadcastLobbyUpdate_nullMatch_noSend() {
        webSocketController.broadcastLobbyUpdate(null);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void broadcastLobbyUpdate_matchWithNullCode_noSend() {
        Match match = mock(Match.class);
        when(match.getCode()).thenReturn(null);

        webSocketController.broadcastLobbyUpdate(match);

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void broadcastLobbyUpdate_validMatch_sendsToCorrectDestination() {
        Match match = mock(Match.class);
        when(match.getCode()).thenReturn("ABC123");

        webSocketController.broadcastLobbyUpdate(match);

        String expectedDest = "/topic/lobby/ABC123";
        verify(messagingTemplate, times(1)).convertAndSend(eq(expectedDest), same(match));
    }

    @Test
    void broadcastGameState_nullCode_noSend() {
        webSocketController.broadcastGameState(null, new Object());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void broadcastGameState_validCode_sendsPayload() {
        String code = "GAME1";
        Object gameState = new Object();

        webSocketController.broadcastGameState(code, gameState);

        String expectedDest = "/topic/game/" + code;
        verify(messagingTemplate, times(1)).convertAndSend(eq(expectedDest), same(gameState));
    }

    @Test
    void broadcastVoteStart_nullCode_noSend() {
        webSocketController.broadcastVoteStart(null, new Object());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void broadcastVoteStart_validCode_sendsPayload() {
        String code = "G2";
        Object voteStart = new Object();

        webSocketController.broadcastVoteStart(code, voteStart);

        String expectedDest = "/topic/game/" + code + "/vote/start";
        verify(messagingTemplate, times(1)).convertAndSend(eq(expectedDest), same(voteStart));
    }

    @Test
    void broadcastVoteResult_nullCode_noSend() {
        webSocketController.broadcastVoteResult(null, new Object());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void broadcastVoteResult_validCode_sendsPayload() {
        String code = "G3";
        Object result = new Object();

        webSocketController.broadcastVoteResult(code, result);

        String expectedDest = "/topic/game/" + code + "/vote/result";
        verify(messagingTemplate, times(1)).convertAndSend(eq(expectedDest), same(result));
    }

    @Test
    void broadcastElimination_nullCode_noSend() {
        webSocketController.broadcastElimination(null, new Object());
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void broadcastElimination_validCode_sendsPayload() {
        String code = "G4";
        Object elim = new Object();

        webSocketController.broadcastElimination(code, elim);

        String expectedDest = "/topic/game/" + code + "/elimination";
        verify(messagingTemplate, times(1)).convertAndSend(eq(expectedDest), same(elim));
    }
}
