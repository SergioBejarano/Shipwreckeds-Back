package com.arsw.shipwreckeds.model;

/**
 * Enumeration describing the lifecycle states of a match.
 * <ul>
 * <li>{@link #WAITING}: players are gathering in the lobby.</li>
 * <li>{@link #STARTED}: the match is in progress.</li>
 * <li>{@link #IN_MEETING}: players are participating in a voting meeting.</li>
 * <li>{@link #FINISHED}: the match has ended.</li>
 * </ul>
 *
 */
public enum MatchStatus {
    WAITING,
    STARTED,
    IN_MEETING,
    FINISHED
}
