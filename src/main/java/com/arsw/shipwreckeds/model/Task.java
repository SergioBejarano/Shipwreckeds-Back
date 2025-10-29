package com.arsw.shipwreckeds.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Base class for interactive tasks in the game.
 * <p>
 * Tasks are positioned on the map, expose an identifier, and track whether they
 * are active. Subclasses implement the
 * mechanics for specific interactions such as repairing the boat or filling
 * fuel.
 *
 */
@Getter
@Setter
public abstract class Task {

    protected Long id;
    protected Position location;
    protected boolean active;

    /**
     * Creates a task anchored to a specific location.
     *
     * @param id       unique task identifier
     * @param location coordinates where the task resides
     */
    public Task(Long id, Position location) {
        this.id = id;
        this.location = location;
        this.active = false;
    }

    /**
     * Activates the task on behalf of the specified player.
     *
     * @param playerId player that started the interaction
     */
    public abstract void startBy(Long playerId);

    /**
     * Updates the task progression based on player actions.
     *
     * @param playerId player responsible for the update
     * @param delta    amount to add (or subtract) from the progress
     */
    public abstract void progressUpdate(Long playerId, double delta);
}
