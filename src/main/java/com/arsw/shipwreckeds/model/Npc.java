package com.arsw.shipwreckeds.model;

import lombok.Getter;
import lombok.Setter;

/**
 * Clase que representa a un NPC (personaje no jugador) dentro del juego.
 * 
 * Los NPCs simulan comportamientos humanos, moviéndose por la isla
 * y realizando acciones básicas para confundir a los jugadores.
 * Comparten la misma apariencia que el infiltrado y pueden ser
 * sospechosos durante la reunión.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */
@Getter
@Setter
public class Npc {

    // Atributos principales
    private Long id;
    private String skinId;
    private Position position;
    private boolean active;
    private double movementSpeed;
    private boolean infiltrator;

    /**
     * Constructor que crea un nuevo NPC con su apariencia y posición inicial.
     * 
     * @param id            identificador único del NPC
     * @param skinId        apariencia del NPC (coincide con la del infiltrado)
     * @param position      posición inicial en el mapa
     * @param movementSpeed velocidad base de movimiento
     */
    public Npc(Long id, String skinId, Position position, double movementSpeed, boolean infiltrator) {
        this.id = id;
        this.skinId = skinId;
        this.position = position;
        this.movementSpeed = movementSpeed;
        this.active = true;
        this.infiltrator = infiltrator;
    }

    /**
     * Mueve al NPC hacia una nueva posición simulando comportamiento autónomo.
     * 
     * @param newX nueva coordenada X
     * @param newY nueva coordenada Y
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
     * Simula el movimiento automático del NPC dentro del mapa.
     * En una versión más avanzada, este método podría ser controlado
     * por un hilo o por un sistema de IA simple.
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
     * Desactiva al NPC (por ejemplo, si fue "expulsado" en la votación).
     */
    public void deactivate() {
        this.active = false;
        System.out.println("El NPC " + id + " ha sido eliminado del juego.");
    }
}
