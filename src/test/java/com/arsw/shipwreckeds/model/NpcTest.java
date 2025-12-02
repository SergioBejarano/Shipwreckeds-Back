package com.arsw.shipwreckeds.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NpcTest {

    @Test
    void moveToUpdatesPositionWhenActive() {
        Position position = new Position(0, 0);
        Npc npc = new Npc(3L, "skin", position, 1.0, false);

        npc.moveTo(5, 7);

        assertEquals(5, npc.getPosition().getX());
        assertEquals(7, npc.getPosition().getY());
        assertEquals("NPC-3", npc.getDisplayName());
    }

    @Test
    void moveToIgnoredWhenInactive() {
        Position position = new Position(1, 1);
        Npc npc = new Npc(4L, "skin", position, 1.0, false);
        npc.deactivate();

        npc.moveTo(10, 10);

        assertEquals(1, npc.getPosition().getX());
        assertEquals(1, npc.getPosition().getY());
        assertFalse(npc.isActive());
    }
}
