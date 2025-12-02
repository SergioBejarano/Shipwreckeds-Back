package com.arsw.shipwreckeds.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PositionTest {

    @Test
    void moveToUpdatesCoordinatesAndToString() {
        Position position = new Position(0, 0);

        position.moveTo(4.5, 2.25);

        assertEquals(4.5, position.getX());
        assertEquals(2.25, position.getY());
        assertEquals("(4.5, 2.25)", position.toString());
    }
}
