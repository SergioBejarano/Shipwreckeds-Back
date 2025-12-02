package com.arsw.shipwreckeds.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FuelWarTaskTest {

    @Test
    void registerClickOnlyCountsWhileActive() {
        FuelWarTask task = new FuelWarTask(1L, new Position(0, 0), 5L);

        task.registerClick(7L);
        assertTrue(task.getClickCounts().isEmpty(), "Inactive task must ignore clicks");

        task.startBy(5L);
        task.registerClick(7L);
        task.registerClick(7L);

        assertEquals(2, task.getClickCounts().get(7L));
        assertTrue(task.isActive(), "Task remains active after registering clicks");
        assertTrue(task.isContested(), "Contest flag must be set once started");
    }

    @Test
    void progressUpdateDelegatesToRegisterClick() {
        FuelWarTask task = new FuelWarTask(2L, new Position(1, 1), 9L);
        task.startBy(9L);

        task.progressUpdate(8L, 0.0);
        task.progressUpdate(9L, 99.0);

        assertEquals(1, task.getClickCounts().get(8L));
        assertEquals(1, task.getClickCounts().get(9L));
    }

    @Test
    void determineWinnerReturnsNullWhenTieOrEmpty() {
        FuelWarTask task = new FuelWarTask(3L, new Position(2, 2), 4L);

        assertNull(task.determineWinner(), "No clicks should return null winner");

        task.startBy(4L);
        task.registerClick(1L);
        task.registerClick(1L);
        task.registerClick(2L);

        assertEquals(1L, task.determineWinner());

        task.getClickCounts().put(2L, 2);
        assertNull(task.determineWinner(), "Tie situations should return null winner");
    }
}
