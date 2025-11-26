package com.arsw.shipwreckeds.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.arsw.shipwreckeds.model.dto.VoteResult;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Aggregates all server-side state for a multiplayer match, including players,
 * NPCs, timers and vote tracking.
 *
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private VoteResult lastVoteResult;
    private long lastVoteResultEpochMs;

    // Winner message broadcast to clients when the match concludes
    private String winnerMessage;

    /**
     * Creates a new lobby with the supplied identifier and code.
     *
     * @param id   sequential identifier assigned by the service layer
     * @param code public alphanumeric code shared with joining players
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
        this.fuelPercentage = 0.0;
        this.winnerMessage = null;
        this.voteStartEpochMs = 0L;
        this.lastVoteResult = null;
        this.lastVoteResultEpochMs = 0L;
    }

    /**
     * Transitions the lobby into the started state if enough players are present.
     */
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

    /**
     * Randomly marks one player as the infiltrator.
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
     * Decrements the match timer and triggers victory resolution when it reaches
     * zero.
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
     * Resolves victory conditions when the timer expires by inspecting infiltrator
     * status and fuel completion.
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
     * Finalizes the match, updating the status and logging the outcome.
     */
    public void endMatch() {
        this.status = MatchStatus.FINISHED;
        System.out.println("La partida ha terminado.");
        if (this.winnerMessage != null) {
            System.out.println(this.winnerMessage);
        }
    }

    // Remaining helper methods retain their original behavior
    /**
     * Applies a delta to the fuel gauge while clamping the value between 0 and 100.
     *
     * @param delta incremental change to apply
     * @return resulting fuel percentage after the adjustment
     */
    public synchronized double adjustFuel(double delta) {
        double updated = Math.max(0.0, Math.min(100.0, this.fuelPercentage + delta));
        this.fuelPercentage = updated;
        return updated;
    }

    /**
     * Resets the fuel gauge to zero.
     */
    public synchronized void resetFuel() {
        this.fuelPercentage = 0.0;
    }

    /**
     * Indicates whether the current fuel cycle allows interaction.
     *
     * @return {@code true} when the window is open
     */
    @JsonIgnore
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

    /**
     * Computes the number of seconds left until the fuel window toggles.
     *
     * @return remaining seconds in the current fuel cycle
     */
    @JsonIgnore
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

    /**
     * @return {@code true} when a vote is currently running
     */
    public boolean isVotingActive() {
        return votingActive;
    }

    /**
     * Opens a voting session and clears previous ballots.
     */
    public void startVoting() {
        this.votingActive = true;
        if (this.votesByPlayer == null)
            this.votesByPlayer = new java.util.concurrent.ConcurrentHashMap<>();
        this.votesByPlayer.clear();
        this.voteStartEpochMs = System.currentTimeMillis();
        this.lastVoteResult = null;
        this.lastVoteResultEpochMs = 0L;
        System.out.println("Votación iniciada para partida " + this.code);
    }

    /**
     * Closes the active voting session.
     */
    public void stopVoting() {
        this.votingActive = false;
        this.voteStartEpochMs = 0L;
    }

    /**
     * Stores or updates the vote for the specified player.
     *
     * @param username voter identifier
     * @param targetId target avatar id (or special flag for abstention)
     */
    public void recordVote(String username, Long targetId) {
        if (this.votesByPlayer == null)
            this.votesByPlayer = new java.util.concurrent.ConcurrentHashMap<>();
        this.votesByPlayer.put(username, targetId);
    }

    /**
     * Lazily initializes and returns the map holding the current ballots.
     *
     * @return map where keys are usernames and values are target ids
     */
    public java.util.Map<String, Long> getVotesByPlayer() {
        if (this.votesByPlayer == null)
            this.votesByPlayer = new java.util.concurrent.ConcurrentHashMap<>();
        return this.votesByPlayer;
    }

    /**
     * Counts living human players (excluding the infiltrator).
     *
     * @return number of alive castaways
     */
    public int countHumanAlivePlayers() {
        return (int) this.players.stream().filter(p -> p.isAlive() && !p.isInfiltrator()).count();
    }

    /**
     * Checks whether every living human has cast a vote.
     *
     * @return {@code true} when all required ballots are present
     */
    public boolean allHumansVoted() {
        return this.votesByPlayer != null && this.votesByPlayer.size() >= countHumanAlivePlayers();
    }

    /**
     * Logs a meeting trigger attempt when the match is active.
     *
     * @param by player initiating the meeting
     */
    public void triggerMeeting(Player by) {
        if (status != MatchStatus.STARTED) {
            System.out.println("No se puede convocar una reunión en este momento.");
            return;
        }

        System.out.println("El jugador " + by.getUsername() + " convocó una reunión.");
        this.status = MatchStatus.STARTED;
    }

    /**
     * Adds a player to the lobby, resetting their state if the match has not
     * started yet.
     *
     * @param player player being added to the roster
     */
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

    /**
     * Appends an NPC to the match roster.
     *
     * @param npc NPC instance to include
     */
    public void addNpc(Npc npc) {
        npcs.add(npc);
    }

    /**
     * Registers a task associated with this match.
     *
     * @param task task instance to add
     */
    public void addTask(Task task) {
        tasks.add(task);
    }
}
