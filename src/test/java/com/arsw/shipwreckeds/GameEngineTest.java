package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.controller.WebSocketController;
import com.arsw.shipwreckeds.model.*;
import com.arsw.shipwreckeds.service.GameEngine;
import com.arsw.shipwreckeds.service.MatchService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Method;
import java.util.List;
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

}
