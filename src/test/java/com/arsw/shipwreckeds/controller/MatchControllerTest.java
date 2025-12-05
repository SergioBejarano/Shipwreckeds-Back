package com.arsw.shipwreckeds.controller;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.MatchStatus;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.Position;
import com.arsw.shipwreckeds.model.dto.FuelActionRequest;
import com.arsw.shipwreckeds.model.dto.FuelActionResponse;
import com.arsw.shipwreckeds.model.dto.GameState;
import com.arsw.shipwreckeds.model.dto.VoteRequest;
import com.arsw.shipwreckeds.model.dto.VoteResult;
import com.arsw.shipwreckeds.model.dto.VoteStart;
import com.arsw.shipwreckeds.service.AuthService;
import com.arsw.shipwreckeds.service.GameEngine;
import com.arsw.shipwreckeds.service.MatchService;
import com.arsw.shipwreckeds.service.NpcService;
import com.arsw.shipwreckeds.service.RoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private MatchController controller;

    @Test
    @DisplayName("El host puede iniciar la partida y se disparan los broadcasts")
    void startMatch_hostLaunchesGame_sendsUpdates() {
        String code = "CODE";
        String host = "host";
        Match match = baseMatchWithPlayers(code, host, 5);

        when(matchService.updateMatch(eq(code), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<Match, Match> mutator = inv.getArgument(1);
            return mutator.apply(match);
        });

        ResponseEntity<Object> response = controller.startMatch(code, host);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(match, response.getBody());
        verify(roleService).assignHumanRoles(match);
        verify(npcService).generateNpcs(match);
        verify(webSocketController).broadcastLobbyUpdate(match);
        verify(webSocketController).broadcastGameState(eq(code), any(GameState.class));
        verify(gameEngine).startMatchTicker(code);
    }

    @Test
    @DisplayName("Un jugador humano puede iniciar votación y se programa el timeout")
    void startVote_validRequest_broadcastsStart() {
        String code = "MATCH";
        String username = "alice";
        Match match = baseMatchWithPlayers(code, username, 5);
        match.setStatus(MatchStatus.STARTED);
        match.setNpcs(new ArrayList<>());

        when(matchService.updateMatch(eq(code), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<Match, ?> fn = inv.getArgument(1);
            return fn.apply(match);
        });

        ResponseEntity<Object> response = controller.startVote(code, username);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(gameEngine).scheduleVoteTimeout(eq(code), any(Runnable.class));
        verify(webSocketController).broadcastGameState(eq(code), any(GameState.class));
        verify(webSocketController).broadcastVoteStart(eq(code), any(VoteStart.class));
    }

    @Test
    @DisplayName("Cuando todos los humanos votan se concluye la votación y se emite el resultado")
    void submitVote_allHumansVoted_triggersConclusion() {
        String code = "VOTE";
        Player human = new Player(1L, "bob", "skin", new Position(0, 0));
        human.setAlive(true);
        Player infiltrator = new Player(2L, "spy", "skin", new Position(5, 0));
        infiltrator.setInfiltrator(true);
        infiltrator.setAlive(true);
        Npc npc = new Npc(200L, "npc", new Position(3, 3), 0.2, false);

        Match match = new Match(1L, code);
        match.setStatus(MatchStatus.STARTED);
        match.setPlayers(new ArrayList<>(List.of(human, infiltrator)));
        match.setNpcs(new ArrayList<>(List.of(npc)));
        match.setInfiltrator(infiltrator);
        match.startVoting();

        AtomicInteger callCount = new AtomicInteger();
        when(matchService.updateMatch(eq(code), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<Match, ?> fn = inv.getArgument(1);
            callCount.incrementAndGet();
            return fn.apply(match);
        });

        VoteRequest req = new VoteRequest(human.getUsername(), npc.getId());
        ResponseEntity<Object> response = controller.submitVote(code, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(callCount.get() >= 2, "Se debe llamar updateMatch para votar y para concluir");
        verify(gameEngine).cancelVoteTimeout(code);
        verify(webSocketController).broadcastVoteResult(eq(code), any(VoteResult.class));
        verify(webSocketController, atLeastOnce()).broadcastGameState(eq(code), any(GameState.class));
        verify(gameEngine, atLeastOnce()).stopMatchTicker(code);
    }

    @Test
    @DisplayName("Completar el tanque detiene el ticker y devuelve el nuevo estado")
    void modifyFuel_fillCompletesMatch() {
        String code = "FUEL";
        Player actor = new Player(1L, "crew", "skin", new Position(112.0, 0.0));
        actor.setAlive(true);

        Match match = spy(new Match(1L, code));
        match.setStatus(MatchStatus.STARTED);
        match.setTimerSeconds(Match.MATCH_DURATION_SECONDS);
        match.setPlayers(new ArrayList<>(List.of(actor)));
        match.setNpcs(new ArrayList<>());
        match.setFuelPercentage(95.0);

        doReturn(true).when(match).isFuelWindowOpenNow();
        doReturn(0).when(match).getFuelWindowSecondsRemaining();

        when(matchService.updateMatch(eq(code), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<Match, ?> fn = inv.getArgument(1);
            return fn.apply(match);
        });

        FuelActionRequest req = new FuelActionRequest();
        req.setUsername(actor.getUsername());
        req.setAction(FuelActionRequest.Action.FILL);
        req.setAmount(10.0);

        ResponseEntity<Object> response = controller.modifyFuel(code, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof FuelActionResponse);
        FuelActionResponse body = (FuelActionResponse) response.getBody();
        assertEquals(100.0, body.getFuelPercentage());
        assertEquals("finished", body.getStatus());
        verify(gameEngine).stopMatchTicker(code);
        verify(webSocketController).broadcastGameState(eq(code), any(GameState.class));
    }

    @Test
    @DisplayName("El infiltrado puede eliminar al último humano y la partida termina")
    void eliminate_lastHumanKilled_finishesMatch() {
        String code = "ELIM";
        Player infiltrator = new Player(1L, "spy", "skin", new Position(0.0, 0.0));
        infiltrator.setInfiltrator(true);
        infiltrator.setAlive(true);
        Player crew = new Player(2L, "crew", "skin", new Position(5.0, 0.0));
        crew.setAlive(true);

        Match match = new Match(1L, code);
        match.setStatus(MatchStatus.STARTED);
        match.setPlayers(new ArrayList<>(List.of(infiltrator, crew)));

        when(matchService.updateMatch(eq(code), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Function<Match, ?> fn = inv.getArgument(1);
            return fn.apply(match);
        });

        VoteRequest req = new VoteRequest(infiltrator.getUsername(), crew.getId());
        ResponseEntity<Object> response = controller.eliminate(code, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(gameEngine).stopMatchTicker(code);
        verify(webSocketController).broadcastElimination(eq(code), any());
        verify(webSocketController).broadcastGameState(eq(code), any(GameState.class));
    }

    private Match baseMatchWithPlayers(String code, String host, int totalPlayers) {
        Match match = new Match(1L, code);
        List<Player> players = new ArrayList<>();
        for (int i = 0; i < totalPlayers; i++) {
            Player p = new Player((long) (i + 1), "player" + i, "skin", new Position(0, 0));
            p.setAlive(true);
            players.add(p);
        }
        players.get(0).setUsername(host);
        match.setPlayers(players);
        match.setNpcs(new ArrayList<>());
        match.setStatus(MatchStatus.WAITING);
        return match;
    }
}
