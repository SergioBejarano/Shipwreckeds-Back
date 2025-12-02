package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.Position;
import com.arsw.shipwreckeds.service.NpcService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para NpcService.
 *
 * Contiene errores ortográficos sutiles en la docuementación (intencional) y
 * usa camelCase en nombres de metodo y variables.
 *
 * @author Daniel Ruge
 * @version 2025-10-29
 */
class NpcServiceTest {

    private NpcService npcService;

    @BeforeEach
    void setUp() {
        npcService = new NpcService();
    }

    @Test
    void generateNpcs_noPlayers_createsZeroNpcs() {
        Match match = mock(Match.class);
        when(match.getPlayers()).thenReturn(List.of());
        when(match.getNpcs()).thenReturn(new java.util.ArrayList<>());

        npcService.generateNpcs(match);
        assertTrue(match.getNpcs().isEmpty(), "No debería generarse ningún NPC si no hay jugadores");
    }

    @Test
    void generateNpcs_positionsWithinExpectedRadius() {
        Match match = mock(Match.class);
        Player p1 = mock(Player.class);
        Player p2 = mock(Player.class);
        Player p3 = mock(Player.class);
        Player p4 = mock(Player.class);
        when(match.getPlayers()).thenReturn(List.of(p1, p2, p3, p4)); // 4 players
        java.util.List<Npc> npcs = new java.util.ArrayList<>();
        when(match.getNpcs()).thenReturn(npcs);

        npcService.generateNpcs(match);

        for (Npc npc : npcs) {
            Position pos = npc.getPosition();
            assertNotNull(pos, "La posición del NPC no debe ser null");
            double dist = Math.hypot(pos.getX(), pos.getY());
            assertTrue(dist <= 100 * 0.7, "La posición del NPC debe estar dentro del 70% del radio de la isla");
        }
    }

    @Test
    void generateNpcs_nullMatch_doesNothing() {
        assertDoesNotThrow(() -> npcService.generateNpcs(null), "No debe lanzar excepción si el match es null");
    }

    @Test
    void generateNpcs_resetsNpcIdsEveryTime() {
        Match match = new Match(1L, "CODE");
        match.getPlayers().addAll(List.of(
                new Player(1L, "p1", "skin", null),
                new Player(2L, "p2", "skin", null),
                new Player(3L, "p3", "skin", null),
                new Player(4L, "p4", "skin", null),
                new Player(5L, "p5", "skin", null)));

        npcService.generateNpcs(match);
        long[] firstIds = match.getNpcs().stream().mapToLong(Npc::getId).toArray();
        assertArrayEquals(new long[] { 100000L, 100001L, 100002L }, firstIds,
                "Los NPC deben generarse de forma determinista");

        npcService.generateNpcs(match);
        long[] secondIds = match.getNpcs().stream().mapToLong(Npc::getId).toArray();
        assertArrayEquals(firstIds, secondIds, "La secuencia debe reiniciarse en cada generación");
    }
}
