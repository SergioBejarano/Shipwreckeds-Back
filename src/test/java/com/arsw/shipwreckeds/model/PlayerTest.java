package com.arsw.shipwreckeds.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PlayerTest {

    @Test
    void constructorInitializesAliveNonInfiltrator() {
        Position spawn = new Position(1, 2);

        Player player = new Player(1L, "captain", "skin", spawn);

        assertTrue(player.isAlive());
        assertFalse(player.isInfiltrator());
        assertSame(spawn, player.getPosition());
    }

    @Test
    void moveToUpdatesPositionReference() {
        Player player = new Player(1L, "captain", "skin", new Position(0, 0));
        Position destination = new Position(5, 5);

        player.moveTo(destination);

        assertSame(destination, player.getPosition());
    }
}
