package com.arsw.shipwreckeds.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a non-player character roaming the island.
 * <p>
 * NPCs mimic human behavior, share the infiltrator's appearance, and are meant
 * to mislead players during meetings.
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class Npc {

    private Long id;
    private String skinId;
    private Position position;
    private boolean active;
    private double movementSpeed;
    private boolean infiltrator;
    private String displayName;

    /**
     * Builds a new NPC with visual traits and starting position.
     *
     * @param id            NPC identifier
     * @param skinId        appearance identifier (mirrors the infiltrator)
     * @param position      initial map position
     * @param movementSpeed base movement speed
     * @param infiltrator   whether this NPC should be flagged as infiltrator themed
     */
    public Npc(Long id, String skinId, Position position, double movementSpeed, boolean infiltrator) {
        this.id = id;
        this.skinId = skinId;
        this.position = position;
        this.movementSpeed = movementSpeed;
        this.active = true;
        this.infiltrator = infiltrator;
        this.displayName = "NPC-" + id;
    }

    /**
     * Moves the NPC to a new position, simulating autonomous behavior.
     *
     * @param newX new X coordinate
     * @param newY new Y coordinate
     */
    public void moveTo(int newX, int newY) {
        if (!active) {
            System.out.println("El NPC " + id + " está inactivo y no puede moverse.");
            return;
        }
        position.moveTo(newX, newY);
        System.out.println("El NPC " + id + " se movió a (" + newX + ", " + newY + ").");
    }

    /**
     * Simulates a random step across the island. Future versions may delegate to an
     * AI system.
     */
    public void performRandomMovement() {
        if (!active)
            return;

        double deltaX = (Math.random() * 2.0 - 1.0); // movimiento en -1..1
        double deltaY = (Math.random() * 2.0 - 1.0);

        double newX = position.getX() + deltaX * movementSpeed;
        double newY = position.getY() + deltaY * movementSpeed;

        moveTo((int) Math.round(newX), (int) Math.round(newY));
    }

    /**
     * Deactivates the NPC, e.g., when expelled during a vote.
     */
    public void deactivate() {
        this.active = false;
        System.out.println("El NPC " + id + " ha sido eliminado del juego.");
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
