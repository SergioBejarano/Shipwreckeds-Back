package com.arsw.shipwreckeds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;

/**
 * Clase que representa una partida (Match) dentro del juego.
 * 
 * Una partida contiene la lista de jugadores, NPCs, tareas activas,
 * el estado actual del juego y el tiempo restante.
 * Además, controla la lógica de inicio, asignación del infiltrado,
 * conteo del tiempo y finalización del juego.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */

@Getter
@Setter
public class Match {

    // Atributos principales
    private Long id;
    private String code;
    private List<Player> players;
    private List<Npc> npcs;
    private List<Task> tasks;
    private MatchStatus status;
    private int timerSeconds;
    private Player infiltrator;

    /**
     * Constructor principal para crear una nueva partida.
     * 
     * @param id   identificador único de la partida
     * @param code código que los jugadores usan para unirse
     */
    public Match(Long id, String code) {
        this.id = id;
        this.code = code;
        this.players = new ArrayList<>();
        this.npcs = new ArrayList<>();
        this.tasks = new ArrayList<>();
        this.status = MatchStatus.WAITING;
        this.timerSeconds = 0;
        this.infiltrator = null;
    }

    /**
     * Inicia la partida una vez que todos los jugadores están listos.
     * Se asigna el infiltrado y se activa el temporizador.
     */
    public void startMatch() {
        if (players.size() < 5) {
            System.out
                    .println("No hay suficientes jugadores para iniciar la partida. Se requieren 5 jugadores humanos.");
            return;
        }
        this.status = MatchStatus.STARTED;
        this.timerSeconds = 15 * 60; // 15 minutos
        System.out.println("La partida ha comenzado. Tiempo restante: " + timerSeconds + " segundos.");
    }

    /**
     * Asigna aleatoriamente un jugador como el infiltrado.
     */
    public void assignInfiltrator() {
        if (players.isEmpty())
            return;

        Random random = new Random();
        int index = random.nextInt(players.size());
        infiltrator = players.get(index);
        infiltrator.setInfiltrator(true);
        System.out.println("El jugador " + infiltrator.getUsername() + " ha sido asignado como infiltrado (oculto).");
    }

    /**
     * Reduce el temporizador de la partida y controla el fin del juego.
     */
    public void tickTimer() {
        if (status != MatchStatus.STARTED)
            return;

        timerSeconds--;
        if (timerSeconds <= 0) {
            endMatch();
        }
    }

    /**
     * Finaliza la partida y anuncia su conclusión.
     */
    public void endMatch() {
        this.status = MatchStatus.FINISHED;
        System.out.println("La partida ha terminado.");
    }

    /**
     * Inicia una reunión solicitada por un jugador.
     * 
     * @param by jugador que convoca la reunión
     */
    public void triggerMeeting(Player by) {
        if (status != MatchStatus.STARTED) {
            System.out.println("No se puede convocar una reunión en este momento.");
            return;
        }

        System.out.println("El jugador " + by.getUsername() + " convocó una reunión.");
        this.status = MatchStatus.STARTED;
        // Aquí se activaría el chat y la votación en el flujo del juego
    }

    /**
     * Agrega un nuevo jugador a la partida.
     * 
     * @param player jugador que se une
     */
    public void addPlayer(Player player) {
        if (status != MatchStatus.WAITING) {
            System.out.println("No se pueden unir más jugadores, la partida ya ha comenzado.");
            return;
        }
        players.add(player);
        System.out.println("Jugador " + player.getUsername() + " se unió a la partida con código " + code + ".");
    }

    /**
     * Agrega un NPC a la partida.
     * 
     * @param npc personaje no jugador que será agregado
     */
    public void addNpc(Npc npc) {
        npcs.add(npc);
    }

    /**
     * Agrega una tarea al entorno de la partida.
     * 
     * @param task tarea que se agregará
     */
    public void addTask(Task task) {
        tasks.add(task);
    }
}
