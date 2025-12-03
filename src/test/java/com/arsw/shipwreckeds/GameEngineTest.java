package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.controller.WebSocketController;
import com.arsw.shipwreckeds.model.*;
import com.arsw.shipwreckeds.service.GameEngine;
import com.arsw.shipwreckeds.service.MatchService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para GameEngine.
 *
 *
 * @author Daniel Ruge
 * @version 2025-10-29
 */
@ExtendWith(MockitoExtension.class)
class GameEngineTest {

    @Mock
    private WebSocketController ws;

    @Mock
    private MatchService matchService;

    private GameEngine gameEngine;

    @BeforeEach
    void setUp() {
        gameEngine = new GameEngine(ws, matchService);
    }

    @AfterEach
    void tearDown() {
        gameEngine.shutdown();
    }

    @Test
    void buildNpcAlias_producesStableAlias() {
        String a1 = GameEngine.buildNpcAlias(1L);
        String a2 = GameEngine.buildNpcAlias(1000L);
        assertEquals("NPC-100001", a1);
        assertEquals("NPC-101000", a2);
    }

    @Test
    void updateNpcMovement_activeNpc_movesPosition() throws Exception {
        // arrange
        Match match = mock(Match.class);
        Npc npc = mock(Npc.class);

        when(match.getCode()).thenReturn("MUP");
        // create a real Position so that updateNpcMovement mutates it
        Position pos = new Position(0.0, 0.0);
        when(npc.getPosition()).thenReturn(pos);
        when(npc.isActive()).thenReturn(true);
        // give the npc a reasonable movement speed
        when(npc.getMovementSpeed()).thenReturn(0.2);

        when(match.getNpcs()).thenReturn(List.of(npc));

        // invoke private method updateNpcMovement(match, deltaSeconds)
        Method m = GameEngine.class.getDeclaredMethod("updateNpcMovement", Match.class, double.class);
        m.setAccessible(true);

        // act: call with deltaSeconds = 1.0
        m.invoke(gameEngine, match, 1.0);

        // assert: position should have changed from (0,0)
        // it might still be very small but must not be exactly 0,0
        assertTrue(Math.abs(pos.getX()) > 1e-9 || Math.abs(pos.getY()) > 1e-9,
                "Se esperaba que la posición del NPC cambiara tras updateNpcMovement");

        // also ensure we didn't attempt to remove the npc (since it's active)
        verify(npc, never()).deactivate();
    }

    @Test
    void updateNpcMovement_outsideIslandMargin_resetsTargetAndSkipsMove() throws Exception {
        Match match = new Match(1L, "EDGE");
        Npc npc = new Npc();
        npc.setId(200L);
        npc.setActive(true);
        npc.setMovementSpeed(0.55);
        Position initial = new Position(99.5, 0.0);
        npc.setPosition(initial);
        match.setNpcs(new ArrayList<>(List.of(npc)));

        Field targetsField = GameEngine.class.getDeclaredField("npcTargetsByMatch");
        targetsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, java.util.Map<Long, Position>> targetsByMatch = (java.util.Map<String, java.util.Map<Long, Position>>) targetsField
                .get(gameEngine);
        java.util.Map<Long, Position> targets = new java.util.concurrent.ConcurrentHashMap<>();
        Position outwardTarget = new Position(150.0, 0.0);
        targets.put(npc.getId(), outwardTarget);
        targetsByMatch.put(match.getCode(), targets);

        Method m = GameEngine.class.getDeclaredMethod("updateNpcMovement", Match.class, double.class);
        m.setAccessible(true);
        m.invoke(gameEngine, match, 1.0);

        assertEquals(99.5, npc.getPosition().getX(), 1e-6);
        assertEquals(0.0, npc.getPosition().getY(), 1e-6);
        assertNotSame(outwardTarget, targets.get(npc.getId()),
                "Cuando se alcanza el borde, se debe regenerar un nuevo objetivo");
    }

    @Test
    void startMatchTicker_blankCode_doesNothing() {
        gameEngine.startMatchTicker(null);
        gameEngine.startMatchTicker("   ");

        verifyNoInteractions(matchService);
        verifyNoInteractions(ws);
    }

    @Test
    void startMatchTicker_startedMatch_emitsStateAndStops() throws Exception {
        String code = "CODE42";
        Match match = new Match(1L, code);
        match.setStatus(MatchStatus.STARTED);
        match.setTimerSeconds(1);
        match.setFuelPercentage(25.0);
        match.getPlayers().add(new Player(1L, "hero", "skin", new Position(0.0, 0.0)));

        CountDownLatch latch = new CountDownLatch(1);

        when(matchService.updateMatch(eq(code), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            Function<Match, Object> mutator = (Function<Match, Object>) invocation.getArgument(1);
            Object result = mutator.apply(match);
            latch.countDown();
            return result;
        });

        gameEngine.startMatchTicker(code);

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Se esperaba al menos un tick del GameEngine");
        verify(matchService, atLeastOnce()).updateMatch(eq(code), any());
        verify(ws, atLeastOnce()).broadcastGameState(eq(code), any());

        gameEngine.stopMatchTicker(code);
    }

    @Test
    void scheduleAndCancelVoteTimeout_doesNotTriggerCallbackImmediately() {
        String code = "VOTE1";
        CountDownLatch latch = new CountDownLatch(1);

        gameEngine.scheduleVoteTimeout(code, latch::countDown);

        assertEquals(1L, latch.getCount(), "El callback no debe ejecutarse inmediatamente");

        gameEngine.cancelVoteTimeout(code);
        assertEquals(1L, latch.getCount(), "El callback cancelado no debe ejecutarse");
    }

    @Test
    void scheduleVoteTimeout_blankCode_isIgnored() {
        CountDownLatch latch = new CountDownLatch(1);

        gameEngine.scheduleVoteTimeout("", latch::countDown);

        assertEquals(1L, latch.getCount());
    }

}
