package com.arsw.shipwreckeds.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RepairCrackTaskTest {

    @Test
    void progressUpdateStopsAtHundredAndDeactivates() {
        RepairCrackTask task = new RepairCrackTask(1L, new Position(0, 0), 5);
        task.startBy(11L);

        task.progressUpdate(11L, 60);
        task.progressUpdate(11L, 50);

        assertEquals(100, task.getProgressPercent(), "Progress must clamp at 100%");
        assertFalse(task.isActive(), "Task should deactivate once completed");
    }

    @Test
    void progressUpdateIgnoredWhenInactive() {
        RepairCrackTask task = new RepairCrackTask(2L, new Position(1, 1), 5);

        task.progressUpdate(5L, 30);

        assertEquals(0, task.getProgressPercent(), "Inactive tasks should ignore progress");
        assertFalse(task.isActive(), "Inactive flag remains false until startBy");
    }

    @Test
    void addProgressDelegatesToProgressUpdate() {
        RepairCrackTask task = new RepairCrackTask(3L, new Position(2, 2), 5);
        task.startBy(15L);

        task.addProgress(15L, 25);

        assertEquals(25, task.getProgressPercent());
    }
}
