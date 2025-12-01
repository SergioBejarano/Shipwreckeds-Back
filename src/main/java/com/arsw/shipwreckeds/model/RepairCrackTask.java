package com.arsw.shipwreckeds.model;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Task that simulates repairing cracks on the ship hull.
 * <p>
 * Players push the progress bar to 100% and may be required to maintain a
 * minimum repair speed depending on the
 * scenario.
 *
 */
@Getter
@Setter
@Slf4j
public class RepairCrackTask extends Task {

    private int progressPercent;
    private int requiredSpeed;

    /**
     * Creates a repair task for the specified location.
     *
     * @param id            task identifier
     * @param location      map coordinates of the crack
     * @param requiredSpeed minimum speed required to succeed
     */
    public RepairCrackTask(Long id, Position location, int requiredSpeed) {
        super(id, location);
        this.progressPercent = 0;
        this.requiredSpeed = requiredSpeed;
    }

    /**
     * Activates the task on behalf of the given player.
     *
     * @param playerId player starting the repair
     */
    @Override
    public void startBy(Long playerId) {
        this.active = true;
        log.info("El jugador {} comenzó a reparar la grieta {}.", playerId, id);
    }

    /**
     * Applies a progress delta driven by the player's action.
     *
     * @param playerId player performing the repair
     * @param delta    percentage to add to the progress bar
     */
    @Override
    public void progressUpdate(Long playerId, double delta) {
        if (!active) {
            log.info("La tarea {} aún no está activa.", id);
            return;
        }

        progressPercent += delta;
        if (progressPercent >= 100) {
            progressPercent = 100;
            active = false;
            log.info("El jugador {} completó la reparación de la grieta {}.", playerId, id);
        } else {
            log.info("Progreso actual: {}%", progressPercent);
        }
    }

    /**
     * Convenience helper to add integer progress to the task.
     *
     * @param playerId player carrying out the action
     * @param amount   percentage points to add
     */
    public void addProgress(Long playerId, int amount) {
        progressUpdate(playerId, amount);
    }
}
