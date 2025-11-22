package com.arsw.shipwreckeds.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a human participant in the match.
 * <p>
 * Tracks username, skin selection, position, and flags indicating whether the
 * player is alive or assigned as the
 * infiltrator.
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class Player {

    private Long id;
    private String username;
    private String skinId;
    private Position position;
    private boolean isInfiltrator;
    private boolean isAlive;

    /**
     * Creates a player with the provided base attributes.
     *
     * @param id       unique identifier
     * @param username player nickname
     * @param skinId   cosmetic selection
     * @param position starting position on the map
     */
    public Player(Long id, String username, String skinId, Position position) {
        this.id = id;
        this.username = username;
        this.skinId = skinId;
        this.position = position;
        this.isInfiltrator = false;
        this.isAlive = true;
    }

    /**
     * Simulates a click action performed by the player. Useful for
     * quick-interaction mini games.
     */
    public void click() {
        System.out.println(username + " hizo un clic en el juego.");
    }

    /**
     * Updates the player's position within the map.
     *
     * @param p destination coordinates
     */
    public void moveTo(Position p) {
        this.position = p;
        System.out.println(username + " se movió a la posición (" + p.getX() + ", " + p.getY() + ").");
    }

    /**
     * Logs the activation of a task.
     *
     * @param taskId identifier of the task being triggered
     */
    public void activateTask(Long taskId) {
        System.out.println(username + " activó la tarea con ID: " + taskId);
    }

    /**
     * Emits a vote during a meeting.
     *
     * @param targetNpcId identifier of the NPC selected for expulsion
     */
    public void castVote(Long targetNpcId) {
        System.out.println(username + " votó para expulsar al NPC con ID: " + targetNpcId);
    }
}
