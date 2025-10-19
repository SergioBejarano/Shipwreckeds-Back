package com.arsw.shipwreckeds.model;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Clase que representa la tarea competitiva de llenar gasolina.
 * 
 * En esta tarea, dos jugadores compiten haciendo clics para intentar
 * dominar el nivel de gasolina compartido. Cada jugador acumula clics
 * y al final se determina el ganador según quién haya contribuido más.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */

@Getter
@Setter
public class FuelWarTask extends Task {

    private Map<Long, Integer> clickCounts;
    private boolean contested;
    private Long initiatorId;

    /**
     * Constructor principal para crear una tarea de guerra de gasolina.
     * 
     * @param id identificador único de la tarea
     * @param location ubicación donde ocurre la tarea
     * @param initiatorId jugador que inició la tarea
     */
    public FuelWarTask(Long id, Position location, Long initiatorId) {
        super(id, location);
        this.clickCounts = new HashMap<>();
        this.contested = false;
        this.initiatorId = initiatorId;
    }

    /**
     * Inicia la tarea por el jugador que la activa.
     * 
     * @param playerId identificador del jugador que comienza la tarea
     */
    @Override
    public void startBy(Long playerId) {
        this.active = true;
        this.contested = true;
        System.out.println("La guerra de gasolina ha comenzado por el jugador " + playerId + ".");
    }

    /**
     * Registra un clic de un jugador durante la contienda.
     * 
     * @param playerId identificador del jugador que hace el clic
     */
    public void registerClick(Long playerId) {
        if (!active) {
            System.out.println("La tarea aún no está activa.");
            return;
        }
        int current = clickCounts.getOrDefault(playerId, 0);
        clickCounts.put(playerId, current + 1);
        System.out.println("Jugador " + playerId + " hizo un clic. Total: " + (current + 1));
    }

    /**
     * Actualiza el progreso de la tarea (en este caso, cuenta clics).
     * 
     * @param playerId jugador que realizó la acción
     * @param delta cantidad de clics a sumar
     */
    @Override
    public void progressUpdate(Long playerId, double delta) {
        registerClick(playerId);
    }

    /**
     * Determina el jugador ganador al finalizar la contienda.
     * 
     * @return ID del jugador con más clics o null si hay empate
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
            System.out.println("La contienda terminó en empate.");
            return null;
        }

        System.out.println("El jugador ganador es " + winnerId + " con " + maxClicks + " clics.");
        return winnerId;
    }

}
