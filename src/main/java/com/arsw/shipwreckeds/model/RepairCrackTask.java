package com.arsw.shipwreckeds.model;


import lombok.Getter;
import lombok.Setter;

/**
 * Clase que representa una tarea de reparación de grietas en el barco.
 * 
 * El jugador debe completar la barra de progreso hasta llegar al 100%.
 * Algunas grietas pueden requerir una velocidad mínima de reparación
 * para considerarse efectivas dentro del juego.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */
@Getter
@Setter
public class RepairCrackTask extends Task {

    private int progressPercent;
    private int requiredSpeed;

    /**
     * Constructor que crea una nueva tarea de reparación.
     * 
     * @param id identificador único de la tarea
     * @param location posición en el mapa donde se encuentra la grieta
     * @param requiredSpeed velocidad mínima de reparación necesaria
     */
    public RepairCrackTask(Long id, Position location, int requiredSpeed) {
        super(id, location);
        this.progressPercent = 0;
        this.requiredSpeed = requiredSpeed;
    }

    /**
     * Inicia la tarea de reparación por parte de un jugador.
     * 
     * @param playerId identificador del jugador que inicia la tarea
     */
    @Override
    public void startBy(Long playerId) {
        this.active = true;
        System.out.println("El jugador " + playerId + " comenzó a reparar la grieta " + id + ".");
    }

    /**
     * Actualiza el progreso de la tarea según la acción del jugador.
     * 
     * @param playerId identificador del jugador que realiza la acción
     * @param delta porcentaje de avance a sumar
     */
    @Override
    public void progressUpdate(Long playerId, double delta) {
        if (!active) {
            System.out.println("La tarea " + id + " aún no está activa.");
            return;
        }

        progressPercent += delta;
        if (progressPercent >= 100) {
            progressPercent = 100;
            active = false;
            System.out.println("El jugador " + playerId + " completó la reparación de la grieta " + id + ".");
        } else {
            System.out.println("Progreso actual: " + progressPercent + "%");
        }
    }

    /**
     * Añade progreso directo a la tarea (atajo para progreso simple).
     * 
     * @param playerId jugador que realiza la acción
     * @param amount cantidad de progreso a añadir
     */
    public void addProgress(Long playerId, int amount) {
        progressUpdate(playerId, amount);
    }
}
