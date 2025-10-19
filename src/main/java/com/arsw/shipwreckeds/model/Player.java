package com.arsw.shipwreckeds.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Clase que representa a un jugador dentro de la partida.
 * 
 * Cada jugador tiene un nombre de usuario, una apariencia (skin),
 * una posición en el mapa y un estado que indica si sigue con vida
 * o si es el infiltrado dentro del grupo.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */
@Getter
@Setter
public class Player {

    // Atributos principales
    private Long id;
    private String username;
    private String skinId;
    private Position position;
    private boolean isInfiltrator;
    private boolean isAlive;

    /**
     * Constructor principal para crear un jugador con sus datos básicos.
     * 
     * @param id identificador único del jugador
     * @param username nombre del jugador
     * @param skinId apariencia seleccionada por el jugador
     * @param position posición inicial en el mapa
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
     * Simula una acción de clic del jugador.
     * Este método podría utilizarse en minijuegos o tareas
     * donde el jugador deba realizar acciones rápidas.
     */
    public void click() {
        System.out.println(username + " hizo un clic en el juego.");
    }

    /**
     * Mueve al jugador hacia una nueva posición dentro del mapa.
     * 
     * @param p nueva posición destino del jugador
     */
    public void moveTo(Position p) {
        this.position = p;
        System.out.println(username + " se movió a la posición (" + p.getX() + ", " + p.getY() + ").");
    }

    /**
     * Activa una tarea disponible en la partida.
     * 
     * @param taskId identificador de la tarea a realizar
     */
    public void activateTask(Long taskId) {
        System.out.println(username + " activó la tarea con ID: " + taskId);
    }

    /**
     * Permite al jugador emitir su voto durante una reunión.
     * 
     * @param targetNpcId identificador del NPC al que desea expulsar
     */
    public void castVote(Long targetNpcId) {
        System.out.println(username + " votó para expulsar al NPC con ID: " + targetNpcId);
    }
}
