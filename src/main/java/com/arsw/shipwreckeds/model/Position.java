package com.arsw.shipwreckeds.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Clase que representa una posición en el mapa del juego.
 * 
 * Cada posición está definida por coordenadas X y Y que indican
 * la ubicación del jugador o de un objeto dentro del entorno.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */

 @Data
 @AllArgsConstructor
public class Position {

    // Coordenadas en el mapa
    private int x;
    private int y;




    /**
     * Mueve la posición a un nuevo punto en el mapa.
     * 
     * @param newX nueva coordenada X
     * @param newY nueva coordenada Y
     */
    public void moveTo(int newX, int newY) {
        this.x = newX;
        this.y = newY;
        System.out.println("Posición actualizada a (" + x + ", " + y + ").");
    }

    /**
     * Retorna la posición como texto legible.
     * 
     * @return coordenadas formateadas como (x, y)
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
