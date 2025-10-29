package com.arsw.shipwreckeds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import lombok.Getter;
import lombok.Setter;

/**
 * Clase que representa una partida (Match) dentro del juego.
 * ...
 * 
 * @version 26/10/2025
 */
@Getter
@Setter
public class Match {

    public static final int MATCH_DURATION_SECONDS = 4 * 60;
    public static final int FUEL_WINDOW_CYCLE_SECONDS = 60;
    public static final int VOTE_DURATION_SECONDS = 20;

    private Long id;
    private String code;
    private List<Player> players;
    private List<Npc> npcs;
    private List<Task> tasks;
    private MatchStatus status;
    private int timerSeconds;
    private Player infiltrator;
    private double fuelPercentage;
    private boolean votingActive;
    private java.util.Map<String, Long> votesByPlayer;
    private long voteStartEpochMs;

    // --- Nuevo campo para comunicar el mensaje final a clientes ---
    private String winnerMessage;

    public Match(Long id, String code) {
        this.id = id;
        this.code = code;
        this.players = new ArrayList<>();
        this.npcs = new ArrayList<>();
        this.tasks = new ArrayList<>();
        this.status = MatchStatus.WAITING;
        this.timerSeconds = 0;
        this.infiltrator = null;
        this.fuelPercentage = 0.0;
        this.winnerMessage = null;
        this.voteStartEpochMs = 0L;
    }

    public void startMatch() {
        if (players.size() < 5) {
            System.out
                    .println("No hay suficientes jugadores para iniciar la partida. Se requieren 5 jugadores humanos.");
            return;
        }
        this.status = MatchStatus.STARTED;
        this.timerSeconds = MATCH_DURATION_SECONDS;
        this.winnerMessage = null;
        System.out.println("La partida ha comenzado. Tiempo restante: " + timerSeconds + " segundos.");
    }

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
            checkVictoryByTime();
        }
    }

    /**
     * Condición de victoria por tiempo:
     * Si el tiempo llega a cero, el infiltrado sigue vivo y el barco (fuel) no está
     * 100%,
     * el infiltrado gana. Si el barco está 100% gana náufragos. Si el infiltrado ya
     * estaba muerto,
     * gana el equipo de náufragos.
     */
    private void checkVictoryByTime() {
        boolean infiltradoVivo = infiltrator != null && infiltrator.isAlive();
        boolean barcoNoReparado = this.fuelPercentage < 100.0;

        if (infiltradoVivo && barcoNoReparado) {
            this.winnerMessage = "El infiltrado ha ganado — los náufragos no lograron reparar el barco a tiempo";
            endMatch();
            return;
        }

        // Si el barco llegó a 100 justo a tiempo => náufragos ganan
        if (this.fuelPercentage >= 100.0) {
            this.winnerMessage = "¡El barco ha sido reparado a tiempo! Los náufragos escapan con éxito";
            endMatch();
            return;
        }

        // Si infiltrado no está vivo y el barco no está reparado -> igualmente gana
        // náufragos
        if (!infiltradoVivo) {
            this.winnerMessage = "El infiltrado fue eliminado antes de que se agotara el tiempo. Los náufragos ganan.";
            endMatch();
        } else {
            // por defecto, si alguna otra condición (seguridad)
            this.winnerMessage = "Partida finalizada";
            endMatch();
        }
    }

    /**
     * Finaliza la partida y anuncia su conclusión.
     */
    public void endMatch() {
        this.status = MatchStatus.FINISHED;
        System.out.println("La partida ha terminado.");
        if (this.winnerMessage != null) {
            System.out.println(this.winnerMessage);
        }
    }

    // --- resto de métodos sin cambios ---
    public synchronized double adjustFuel(double delta) {
        double updated = Math.max(0.0, Math.min(100.0, this.fuelPercentage + delta));
        this.fuelPercentage = updated;
        return updated;
    }

    public synchronized void resetFuel() {
        this.fuelPercentage = 0.0;
    }

    public synchronized boolean isFuelWindowOpenNow() {
        if (status != MatchStatus.STARTED || timerSeconds <= 0) {
            return false;
        }
        int elapsed = MATCH_DURATION_SECONDS - timerSeconds;
        if (elapsed < 0) {
            elapsed = 0;
        }
        int windowIndex = elapsed / FUEL_WINDOW_CYCLE_SECONDS;
        return windowIndex % 2 == 1;
    }

    public synchronized int getFuelWindowSecondsRemaining() {
        if (status != MatchStatus.STARTED || timerSeconds <= 0) {
            return 0;
        }
        int elapsed = MATCH_DURATION_SECONDS - timerSeconds;
        if (elapsed < 0) {
            elapsed = 0;
        }
        int remainder = elapsed % FUEL_WINDOW_CYCLE_SECONDS;
        int remaining = FUEL_WINDOW_CYCLE_SECONDS - remainder;
        if (remaining <= 0) {
            remaining = FUEL_WINDOW_CYCLE_SECONDS;
        }
        if (timerSeconds < remaining) {
            remaining = timerSeconds;
        }
        return Math.max(0, remaining);
    }

    public boolean isVotingActive() {
        return votingActive;
    }

    public void startVoting() {
        this.votingActive = true;
        if (this.votesByPlayer == null)
            this.votesByPlayer = new java.util.concurrent.ConcurrentHashMap<>();
        this.votesByPlayer.clear();
        this.voteStartEpochMs = System.currentTimeMillis();
        System.out.println("Votación iniciada para partida " + this.code);
    }

    public void stopVoting() {
        this.votingActive = false;
        this.voteStartEpochMs = 0L;
    }

    public void recordVote(String username, Long targetId) {
        if (this.votesByPlayer == null)
            this.votesByPlayer = new java.util.concurrent.ConcurrentHashMap<>();
        this.votesByPlayer.put(username, targetId);
    }

    public java.util.Map<String, Long> getVotesByPlayer() {
        if (this.votesByPlayer == null)
            this.votesByPlayer = new java.util.concurrent.ConcurrentHashMap<>();
        return this.votesByPlayer;
    }

    public int countHumanAlivePlayers() {
        return (int) this.players.stream().filter(p -> p.isAlive() && !p.isInfiltrator()).count();
    }

    public boolean allHumansVoted() {
        return this.votesByPlayer != null && this.votesByPlayer.size() >= countHumanAlivePlayers();
    }

    public void triggerMeeting(Player by) {
        if (status != MatchStatus.STARTED) {
            System.out.println("No se puede convocar una reunión en este momento.");
            return;
        }

        System.out.println("El jugador " + by.getUsername() + " convocó una reunión.");
        this.status = MatchStatus.STARTED;
    }

    public void addPlayer(Player player) {
        if (status != MatchStatus.WAITING) {
            System.out.println("No se pueden unir más jugadores, la partida ya ha comenzado.");
            return;
        }
        if (player != null) {
            player.setAlive(true);
            player.setInfiltrator(false);
            player.setPosition(null);
        }
        players.add(player);
        System.out.println("Jugador " + player.getUsername() + " se unió a la partida con código " + code + ".");
    }

    public void addNpc(Npc npc) {
        npcs.add(npc);
    }

    public void addTask(Task task) {
        tasks.add(task);
    }
}
