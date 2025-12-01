package com.arsw.shipwreckeds.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Competitive task where players race to fill a shared fuel gauge.
 * <p>
 * Participants register clicks to assert control over the gauge. The winner is
 * the player with the highest number of
 * recorded clicks when the task ends.
 *
 */

@Getter
@Setter
@Slf4j
public class FuelWarTask extends Task {

    private Map<Long, Integer> clickCounts;
    private boolean contested;
    private Long initiatorId;

    /**
     * Creates a new fuel war task anchored at a specific location.
     *
     * @param id          unique task identifier
     * @param location    in-game location where the task can be triggered
     * @param initiatorId player that initiated the contest
     */
    public FuelWarTask(Long id, Position location, Long initiatorId) {
        super(id, location);
        this.clickCounts = new HashMap<>();
        this.contested = false;
        this.initiatorId = initiatorId;
    }

    /**
     * Activates the contest on behalf of the triggering player.
     *
     * @param playerId identifier of the player that started the task
     */
    @Override
    public void startBy(Long playerId) {
        this.active = true;
        this.contested = true;
        log.info("La guerra de gasolina ha comenzado por el jugador {}.", playerId);
    }

    /**
     * Registers a new click for the given participant.
     *
     * @param playerId participant adding a click
     */
    public void registerClick(Long playerId) {
        if (!active) {
            log.info("La tarea {} aún no está activa.", id);
            return;
        }
        int current = clickCounts.getOrDefault(playerId, 0);
        clickCounts.put(playerId, current + 1);
        log.info("Jugador {} hizo un clic. Total: {}", playerId, (current + 1));
    }

    /**
     * Called when the task progression changes; delegates to
     * {@link #registerClick(Long)}.
     *
     * @param playerId player performing the action
     * @param delta    ignored delta, maintained for task compatibility
     */
    @Override
    public void progressUpdate(Long playerId, double delta) {
        registerClick(playerId);
    }

    /**
     * Evaluates the collected clicks and returns the leading player.
     *
     * @return player id with the most clicks, or {@code null} when tied
     */
    public Long determineWinner() {
        if (clickCounts.isEmpty()) {
            return null;
        }

        Long winnerId = null;
        int maxClicks = -1;
        boolean tie = false;

        for (Map.Entry<Long, Integer> entry : clickCounts.entrySet()) {
            if (entry.getValue() > maxClicks) {
                maxClicks = entry.getValue();
                winnerId = entry.getKey();
                tie = false;
            } else if (entry.getValue() == maxClicks) {
                tie = true;
            }
        }

        if (tie) {
            log.info("La contienda terminó en empate.");
            return null;
        }

        log.info("El jugador ganador es {} con {} clics.", winnerId, maxClicks);
        return winnerId;
    }

}
