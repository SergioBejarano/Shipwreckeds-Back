package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.controller.MatchController;
import com.arsw.shipwreckeds.controller.WebSocketController;
import com.arsw.shipwreckeds.model.*;
import com.arsw.shipwreckeds.model.dto.*;
import com.arsw.shipwreckeds.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para MatchController.
 *
 * Contiene errores ortográficos sutiles en la docuementación (intencional) y usa
 * camelCase en nombres de metodo y variables.
 *
 * @author Daniel Ruge
 * @version 2025-10-29
 */
@ExtendWith(MockitoExtension.class)
class MatchControllerTest {

    @Mock
    private MatchService matchService;

    @Mock
    private AuthService authService;

    @Mock
    private WebSocketController webSocketController;

    @Mock
    private RoleService roleService;

    @Mock
    private NpcService npcService;

    @Mock
    private GameEngine gameEngine;

    @InjectMocks
    private MatchController matchController;

    @BeforeEach
    void setUp() {
        // inyectado por @InjectMocks
    }

    @Test
    void createMatch_hostNotConnected_returnsBadRequest() {
        CreateMatchRequest req = new CreateMatchRequest();
        req.setHostName("noHost");
        when(authService.getPlayer("noHost")).thenReturn(null);

        ResponseEntity<?> resp = matchController.createMatch(req);

        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(((String) resp.getBody()).toLowerCase().contains("inicia sesión") || ((String) resp.getBody()).toLowerCase().contains("inicia sesion"));
        verify(matchService, never()).createMatch(any());
    }

    @Test
    void joinMatch_success_broadcastsAndReturnsMatch() {
        JoinMatchRequest req = new JoinMatchRequest();
        req.setCode("C1");
        req.setUsername("playerA");

        Player player = mock(Player.class);
        Match returnedMatch = mock(Match.class);

        when(authService.getPlayer("playerA")).thenReturn(player);
        when(matchService.joinMatch("C1", player)).thenReturn(returnedMatch);

        ResponseEntity<?> resp = matchController.joinMatch(req);

        assertEquals(200, resp.getStatusCodeValue());
        assertSame(returnedMatch, resp.getBody());
        verify(matchService, times(1)).joinMatch("C1", player);
        verify(webSocketController, times(1)).broadcastLobbyUpdate(returnedMatch);
    }

    @Test
    void joinMatch_playerNotConnected_returnsBadRequest() {
        JoinMatchRequest req = new JoinMatchRequest();
        req.setCode("C2");
        req.setUsername("ghost");

        when(authService.getPlayer("ghost")).thenReturn(null);

        ResponseEntity<?> resp = matchController.joinMatch(req);

        assertEquals(400, resp.getStatusCodeValue());
        verify(matchService, never()).joinMatch(anyString(), any());
    }

    @Test
    void startMatch_notFound_returnsBadRequest() {
        when(matchService.getMatchByCode("X")).thenReturn(null);

        ResponseEntity<?> resp = matchController.startMatch("X", "host");

        assertEquals(400, resp.getStatusCodeValue());
    }

    @Test
    void startMatch_onlyHostCanStart_returnsForbidden() {
        Match match = mock(Match.class);
        Player first = mock(Player.class);
        when(matchService.getMatchByCode("M")).thenReturn(match);
        when(match.getPlayers()).thenReturn(List.of(first)); // only one player
        when(first.getUsername()).thenReturn("otherHost");

        ResponseEntity<?> resp = matchController.startMatch("M", "notHost");

        assertEquals(403, resp.getStatusCodeValue());
    }


    @Test
    void startMatch_success_assignsRolesGeneratesNpcsBroadcastsAndStartsTicker() {
        Match match = mock(Match.class);
        // 5 players required
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);
        Player p4 = mock(Player.class);
        Player p5 = mock(Player.class);
        List<Player> players = List.of(p1, p2, p3, p4, p5);

        when(matchService.getMatchByCode("MOK")).thenReturn(match);
        when(match.getPlayers()).thenReturn(players);
        when(p1.getUsername()).thenReturn("host123"); // host must be first
        // return empty npc list initially
        when(match.getNpcs()).thenReturn(new ArrayList<>());

        // act
        ResponseEntity<?> resp = matchController.startMatch("MOK", "host123");

        // assert
        assertEquals(200, resp.getStatusCodeValue());
        verify(roleService, times(1)).assignHumanRoles(match);
        verify(npcService, times(1)).generateNpcs(match);
        verify(match, times(1)).startMatch();
        verify(webSocketController, times(1)).broadcastLobbyUpdate(match);
        verify(webSocketController, times(1)).broadcastGameState(eq(match.getCode()), any());
        verify(gameEngine, times(1)).startMatchTicker(match);
    }

    @Test
    void eliminate_outOfRange_returnsForbidden() {
        Match match = mock(Match.class);
        Player killer = mock(Player.class);
        Player target = mock(Player.class);

        when(matchService.getMatchByCode("EL")).thenReturn(match);
        when(match.getStatus()).thenReturn(MatchStatus.STARTED);

        when(match.getPlayers()).thenReturn(List.of(killer, target));

        when(killer.getUsername()).thenReturn("killer");
        when(killer.isAlive()).thenReturn(true);
        when(killer.isInfiltrator()).thenReturn(true);

        when(target.getId()).thenReturn(2L);
        when(target.isAlive()).thenReturn(true);
        when(target.isInfiltrator()).thenReturn(false);

        // set positions far apart: killer at (0,0), target at (1000, 1000)
        when(killer.getPosition()).thenReturn(new Position(0.0, 0.0));
        when(target.getPosition()).thenReturn(new Position(1000.0, 1000.0));

        VoteRequest req = new VoteRequest();
        req.setUsername("killer");
        req.setTargetId(2L);

        ResponseEntity<?> resp = matchController.eliminate("EL", req);

        assertEquals(403, resp.getStatusCodeValue());
        verify(target, never()).setAlive(false);
    }

    @Test
    void eliminate_success_killerIsInfiltrator_eliminatesAndBroadcasts() {
        Match match = mock(Match.class);
        Player killer = mock(Player.class);
        Player target = mock(Player.class);

        when(matchService.getMatchByCode("EL2")).thenReturn(match);
        when(match.getStatus()).thenReturn(MatchStatus.STARTED);

        when(match.getPlayers()).thenReturn(List.of(killer, target));

        when(killer.getUsername()).thenReturn("killer");
        when(killer.isAlive()).thenReturn(true);
        when(killer.isInfiltrator()).thenReturn(true);
        when(killer.getPosition()).thenReturn(new Position(0.0, 0.0));

        when(target.getId()).thenReturn(20L);
        when(target.isAlive()).thenReturn(true);
        when(target.isInfiltrator()).thenReturn(false);
        when(target.getPosition()).thenReturn(new Position(5.0, 5.0)); // within elimination range (≈7.07 < 20)

        VoteRequest req = new VoteRequest();
        req.setUsername("killer");
        req.setTargetId(20L);

        ResponseEntity<?> resp = matchController.eliminate("EL2", req);

        assertEquals(200, resp.getStatusCodeValue());
        // verify that target was marked dead
        verify(target, times(1)).setAlive(false);
        // verify that elimination and gamestate broadcasts happened
        verify(webSocketController, times(1)).broadcastElimination(eq("EL2"), any(EliminationEvent.class));
        verify(webSocketController, times(1)).broadcastGameState(eq("EL2"), any(GameState.class));
    }

    @Test
    void modifyFuel_tooFarFromBoat_returnsForbidden() {
        Match match = mock(Match.class);
        when(matchService.getMatchByCode("F1")).thenReturn(match);
        when(match.getStatus()).thenReturn(MatchStatus.STARTED);

        Player actor = mock(Player.class);
        when(match.getPlayers()).thenReturn(List.of(actor));
        when(actor.getUsername()).thenReturn("actor");
        when(actor.isAlive()).thenReturn(true);
        // actor position far from boat: boat is at (112,0) approx; put actor far
        when(actor.getPosition()).thenReturn(new Position(10000.0, 10000.0));

        FuelActionRequest req = new FuelActionRequest();
        req.setUsername("actor");
        req.setAction(FuelActionRequest.Action.FILL);

        ResponseEntity<?> resp = matchController.modifyFuel("F1", req);

        assertEquals(403, resp.getStatusCodeValue());
        verify(webSocketController, never()).broadcastGameState(anyString(), any());
    }

    @Test
    void modifyFuel_success_whenWindowOpen_updatesAndBroadcasts() {
        Match match = mock(Match.class);
        when(matchService.getMatchByCode("F2")).thenReturn(match);
        when(match.getStatus()).thenReturn(MatchStatus.STARTED);

        Player actor = mock(Player.class);
        when(match.getPlayers()).thenReturn(List.of(actor));
        when(actor.getUsername()).thenReturn("actor");
        when(actor.isAlive()).thenReturn(true);
        // position near boat: boat at ~ (112,0), set actor at (112, 1)
        when(actor.getPosition()).thenReturn(new Position(112.0, 1.0));
        // fuel window open
        when(match.isFuelWindowOpenNow()).thenReturn(true);
        when(match.getStatus()).thenReturn(MatchStatus.STARTED);

        // stub match.fuel behavior
        when(match.getFuelPercentage()).thenReturn(10.0);
        // simulate adjustFuel -> returns updated value (mock)
        when(match.adjustFuel(anyDouble())).thenReturn(15.0);
        when(match.getStatus()).thenReturn(MatchStatus.STARTED);

        FuelActionRequest req = new FuelActionRequest();
        req.setUsername("actor");
        req.setAction(FuelActionRequest.Action.FILL);
        req.setAmount(5.0);

        ResponseEntity<?> resp = matchController.modifyFuel("F2", req);

        assertEquals(200, resp.getStatusCodeValue());
        assertTrue(resp.getBody() instanceof FuelActionResponse);
        FuelActionResponse far = (FuelActionResponse) resp.getBody();
        assertEquals(15.0, far.getFuelPercentage());
        // broadcast happened
        verify(webSocketController, times(1)).broadcastGameState(eq("F2"), any(GameState.class));
    }
}
