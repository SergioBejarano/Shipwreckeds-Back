package com.arsw.shipwreckeds.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Clase abstracta que representa una tarea dentro del juego.
 * 
 * Cada tarea tiene una ubicación en el mapa, un identificador y un
 * estado que indica si está activa o no. Los jugadores pueden iniciarla
 * y realizar acciones que modifiquen su progreso.
 * 
 * Esta clase servirá como base para otros tipos de tareas
 * más específicas, como reparar el barco o llenar gasolina.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */
@Getter
@Setter
public abstract class Task {

    // Atributos principales
    protected Long id;
    protected Position location;
    protected boolean active;

    /**
     * Constructor principal para definir una tarea en una posición específica.
     * 
     * @param id identificador único de la tarea
     * @param location posición dentro del mapa donde se encuentra la tarea
     */
    public Task(Long id, Position location) {
        this.id = id;
        this.location = location;
        this.active = false;
    }


    /**
     * Inicia la tarea por parte de un jugador.
     * 
     * @param playerId identificador del jugador que inicia la tarea
     */
    public abstract void startBy(Long playerId);

    /**
     * Actualiza el progreso de la tarea según una acción del jugador.
     * 
     * @param playerId identificador del jugador que realiza la acción
     * @param delta cantidad de progreso a sumar o restar
     */
    public abstract void progressUpdate(Long playerId, double delta);
}
