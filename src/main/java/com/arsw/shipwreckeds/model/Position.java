package com.arsw.shipwreckeds.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Represents a point within the island map using X and Y coordinates.
 *
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class Position {

    private double x;
    private double y;

    /**
     * Moves the position to a new point within the map.
     *
     * @param newX new X coordinate
     * @param newY new Y coordinate
     */
    public void moveTo(double newX, double newY) {
        this.x = newX;
        this.y = newY;
        log.info("Posición actualizada a ({}, {}).", x, y);
    }

    /**
     * @return formatted coordinates in the form {@code (x, y)}
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
