package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.controller.GameController;
import com.arsw.shipwreckeds.controller.WebSocketController;
import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.MatchStatus;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.Position;
import com.arsw.shipwreckeds.model.dto.MoveCommand;
import com.arsw.shipwreckeds.service.AuthService;
import com.arsw.shipwreckeds.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.argThat;

/**
 * Pruebas unitarias para GameController.
 *
 * Nota: docuementación con errores ortográficos sutiles (intencional) y uso de
 * camelCase en nombres de método y variables.
 *
 * @author Daniel Ruge
 * @version 2025-10-29
 */
@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock
    private MatchService matchService;

    @Mock
    private AuthService authService;

    @Mock
    private WebSocketController webSocketController;

    @InjectMocks
    private GameController gameController;

    @BeforeEach
    void setUp() {
        // gameController se inyecta por @InjectMocks
    }

    @Test
    void handleMove_validMove_callsBroadcastAndMoveTo() {
        // arrange
        String code = "ABC";
        Long avatarId = 1L;
        String username = "player1";

        Match match = mock(Match.class);
        Player target = mock(Player.class);
        MoveCommand cmd = mock(MoveCommand.class);
        MoveCommand.Direction dir = mock(MoveCommand.Direction.class);

        when(matchService.getMatchByCode(code)).thenReturn(match);
        when(match.getStatus()).thenReturn(MatchStatus.STARTED);
        when(match.getPlayers()).thenReturn(List.of(target));
        when(match.getNpcs()).thenReturn(Collections.emptyList());
        when(target.getId()).thenReturn(avatarId);
        when(target.getUsername()).thenReturn(username);
        // initial position (0,0)
        Position initPos = new Position(0.0, 0.0);
        when(target.getPosition()).thenReturn(initPos);
        when(target.isInfiltrator()).thenReturn(false);
        when(target.isAlive()).thenReturn(true);

        when(authService.getPlayer(username)).thenReturn(mock(Player.class));

        when(cmd.getUsername()).thenReturn(username);
        when(cmd.getAvatarId()).thenReturn(avatarId);
        when(cmd.getDirection()).thenReturn(dir);
        // direction (1, 0)
        when(dir.getDx()).thenReturn(1.0);
        when(dir.getDy()).thenReturn(0.0);

        // act
        gameController.handleMove(code, cmd);

        // assert: verify that player.moveTo(...) was called with a Position with x > 0
        verify(target, times(1)).moveTo(argThat(p -> {
            if (p instanceof Position) {
                Position pos = (Position) p;
                // step = ISLAND_RADIUS * 0.035 ≈ 3.5 (ISLAND_RADIUS = 100)
                // so x should be roughly 3.5 (allow small tolerance)
                return Math.abs(pos.getX() - (100.0 * 0.035)) < 0.0001 && Math.abs(pos.getY() - 0.0) < 1e-6;
            }
            return false;
        }));

        // and verify broadcast called once
        verify(webSocketController, times(1)).broadcastGameState(eq(code), ArgumentMatchers.any());
    }

    @Test
    void handleMove_matchNotFound_noBroadcast() {
        String code = "NOPE";
        MoveCommand cmd = mock(MoveCommand.class);

        when(cmd.getUsername()).thenReturn("u");
        when(cmd.getAvatarId()).thenReturn(1L);
        when(cmd.getDirection()).thenReturn(mock(MoveCommand.Direction.class));
        when(matchService.getMatchByCode(code)).thenReturn(null);

        gameController.handleMove(code, cmd);

        verifyNoInteractions(webSocketController);
    }

    @Test
    void handleMove_matchNotStarted_noBroadcast() {
        String code = "CODE";
        MoveCommand cmd = mock(MoveCommand.class);
        Match match = mock(Match.class);

        when(matchService.getMatchByCode(code)).thenReturn(match);
        when(match.getStatus()).thenReturn(MatchStatus.WAITING);

        when(cmd.getUsername()).thenReturn("u");
        when(cmd.getAvatarId()).thenReturn(1L);
        when(cmd.getDirection()).thenReturn(mock(MoveCommand.Direction.class));

        gameController.handleMove(code, cmd);

        verifyNoInteractions(webSocketController);
    }


    @Test
    void handleMove_rateLimited_secondImmediateCallIgnored() {
        // arrange
        String code = "RATE";
        Long avatarId = 10L;
        String username = "rater";
        Match match = mock(Match.class);
        Player target = mock(Player.class);
        MoveCommand cmd1 = mock(MoveCommand.class);
        MoveCommand cmd2 = mock(MoveCommand.class);
        MoveCommand.Direction dir = mock(MoveCommand.Direction.class);

        when(matchService.getMatchByCode(code)).thenReturn(match);
        when(match.getStatus()).thenReturn(MatchStatus.STARTED);
        when(match.getPlayers()).thenReturn(List.of(target));
        when(match.getNpcs()).thenReturn(Collections.emptyList());
        when(target.getId()).thenReturn(avatarId);
        when(target.getUsername()).thenReturn(username);
        when(target.getPosition()).thenReturn(new Position(0.0, 0.0));
        when(target.isInfiltrator()).thenReturn(false);
        when(target.isAlive()).thenReturn(true);

        when(authService.getPlayer(username)).thenReturn(mock(Player.class));

        when(cmd1.getUsername()).thenReturn(username);
        when(cmd1.getAvatarId()).thenReturn(avatarId);
        when(cmd1.getDirection()).thenReturn(dir);

        when(cmd2.getUsername()).thenReturn(username);
        when(cmd2.getAvatarId()).thenReturn(avatarId);
        when(cmd2.getDirection()).thenReturn(dir);

        when(dir.getDx()).thenReturn(1.0);
        when(dir.getDy()).thenReturn(0.0);

        // act: first call should be processed
        gameController.handleMove(code, cmd1);
        // immediate second call should be rate-limited and ignored
        gameController.handleMove(code, cmd2);

        // assert
        verify(webSocketController, times(1)).broadcastGameState(eq(code), ArgumentMatchers.any());
    }

}
