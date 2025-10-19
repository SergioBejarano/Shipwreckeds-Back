package com.arsw.shipwreckeds.model;

/**
 * Enumeración que representa los posibles estados de una partida.
 * 
 * WAITING     → La partida está esperando a que se unan los jugadores.
 * STARTED     → La partida ha comenzado y está en curso.
 * IN_MEETING  → Los jugadores se encuentran en una reunión de votación.
 * FINISHED    → La partida ha finalizado.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */
public enum MatchStatus {
    WAITING,
    STARTED,
    IN_MEETING,
    FINISHED
}
