package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.controller.GameController;
import com.arsw.shipwreckeds.controller.WebSocketController;
import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.MatchStatus;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.Position;
import com.arsw.shipwreckeds.model.dto.MoveCommand;
import com.arsw.shipwreckeds.model.dto.GameState;
import com.arsw.shipwreckeds.service.AuthService;
import com.arsw.shipwreckeds.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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
    @DisplayName("Cuando el movimiento es válido se actualiza la posición y se emite broadcast")
    void handleMove_validMove_updatesStateAndBroadcasts() {
        String code = "ABC";
        String username = "player1";
        long avatarId = 1L;

        Match match = matchWithSinglePlayer(code, avatarId, username, MatchStatus.STARTED);
        MoveCommand cmd = new MoveCommand(username, avatarId, new MoveCommand.Direction(1.0, 0.0));
        when(authService.getPlayer(username)).thenReturn(new Player());
        stubMatchMutation(code, match);

        gameController.handleMove(code, cmd);

        Position updated = match.getPlayers().get(0).getPosition();
        assertNotNull(updated, "La posición debe actualizarse");
        assertEquals(100.0 * 0.035, updated.getX(), 1e-6);
        assertEquals(0.0, updated.getY(), 1e-6);
        verify(webSocketController).broadcastGameState(eq(code), any());
    }

    @Test
    @DisplayName("Si el código de partida no existe no se emite broadcast")
    void handleMove_matchNotFound_noBroadcast() {
        String code = "NOPE";
        MoveCommand cmd = new MoveCommand("u", 99L, new MoveCommand.Direction(1.0, 0.0));
        when(authService.getPlayer("u")).thenReturn(new Player());
        when(matchService.updateMatch(eq(code), any())).thenThrow(new IllegalArgumentException("invalid"));

        gameController.handleMove(code, cmd);

        verifyNoInteractions(webSocketController);
    }

    @Test
    @DisplayName("Si la partida no está en curso, no se mueve ni se notifica")
    void handleMove_matchNotStarted_noBroadcast() {
        String code = "CODE";
        String username = "u";
        long avatarId = 3L;
        Match match = matchWithSinglePlayer(code, avatarId, username, MatchStatus.WAITING);
        MoveCommand cmd = new MoveCommand(username, avatarId, new MoveCommand.Direction(0.0, 1.0));
        when(authService.getPlayer(username)).thenReturn(new Player());
        stubMatchMutation(code, match);

        gameController.handleMove(code, cmd);

        Position position = match.getPlayers().get(0).getPosition();
        assertNull(position, "La posición no debe cambiar cuando la partida no ha iniciado");
        verify(webSocketController, never()).broadcastGameState(eq(code), any());
    }

    @Test
    @DisplayName("El rate limit evita broadcasts consecutivos demasiado rápidos")
    void handleMove_rateLimited_secondImmediateCallIgnored() {
        String code = "RATE";
        String username = "rater";
        long avatarId = 10L;
        Match match = matchWithSinglePlayer(code, avatarId, username, MatchStatus.STARTED);
        MoveCommand first = new MoveCommand(username, avatarId, new MoveCommand.Direction(1.0, 0.0));
        MoveCommand second = new MoveCommand(username, avatarId, new MoveCommand.Direction(1.0, 0.0));
        when(authService.getPlayer(username)).thenReturn(new Player());
        stubMatchMutation(code, match);

        gameController.handleMove(code, first);
        gameController.handleMove(code, second);

        verify(webSocketController).broadcastGameState(eq(code), any());
    }

    @Test
    @DisplayName("Las peticiones incompletas se descartan sin tocar servicios")
    void handleMove_nullPayload_returnsEarly() {
        gameController.handleMove("CODE", null);

        verifyNoInteractions(matchService, authService, webSocketController);
    }

    @Test
    @DisplayName("buildGameState incluye opciones de votación y metadatos")
    void buildGameState_includesVotingMetadata() throws Exception {
        Match match = new Match(1L, "CODE");
        match.setStatus(MatchStatus.STARTED);
        match.setTimerSeconds(42);
        match.setVotingActive(true);
        match.setVoteStartEpochMs(1_000L);

        Player human = new Player(1L, "human", "skin", new Position(10, 5));
        human.setAlive(true);
        Player infiltrator = new Player(2L, "spy", "skin", new Position(15, -3));
        infiltrator.setInfiltrator(true);
        infiltrator.setAlive(true);
        match.setPlayers(new ArrayList<>(List.of(human, infiltrator)));

        Npc npc = new Npc(100L, "npc", new Position(1, 1), 0.3, false);
        match.setNpcs(new ArrayList<>(List.of(npc)));

        Method m = GameController.class.getDeclaredMethod("buildGameState", Match.class);
        m.setAccessible(true);
        GameState state = (GameState) m.invoke(gameController, match);

        assertTrue(state.isVotingActive());
        assertNotNull(state.getVoteOptions(), "Las opciones de votación deben incluirse cuando votingActive es true");
        assertEquals(2, state.getVoteOptions().size(), "Debe exponer NPCs y al infiltrado como opciones");
        assertEquals(match.getCode(), state.getCode());
        assertEquals(match.getTimerSeconds(), state.getTimerSeconds());
    }

    private void stubMatchMutation(String code, Match match) {
        when(matchService.updateMatch(eq(code), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<Match, Object> mutator = (Function<Match, Object>) invocation.getArgument(1);
            return mutator.apply(match);
        });
    }

    private Match matchWithSinglePlayer(String code, long avatarId, String username, MatchStatus status) {
        Match match = new Match(1L, code);
        match.setStatus(status);
        Player player = new Player(avatarId, username, "skin", null);
        player.setAlive(true);
        player.setInfiltrator(false);
        List<Player> roster = new ArrayList<>();
        roster.add(player);
        match.setPlayers(roster);
        match.setNpcs(new ArrayList<>());
        return match;
    }

}
