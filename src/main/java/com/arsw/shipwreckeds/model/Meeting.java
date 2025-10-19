package com.arsw.shipwreckeds.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Clase que representa una reunión dentro de una partida.
 * 
 * Durante la reunión, los jugadores pueden enviar mensajes por chat
 * y votar para expulsar a un NPC sospechoso. Al finalizar el tiempo,
 * los votos son contados y se determina el NPC con más votos.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */
@Setter
@Getter
public class Meeting {

    // Atributos principales
    private Long id;
    private Player calledBy;
    private int durationSeconds;
    private List<ChatMessage> chatMessages;
    private Map<Long, Long> votes;

    /**
     * Constructor principal de la reunión.
     * 
     * @param id identificador único de la reunión
     * @param calledBy jugador que convocó la reunión
     * @param durationSeconds duración de la reunión en segundos
     */
    public Meeting(Long id, Player calledBy, int durationSeconds) {
        this.id = id;
        this.calledBy = calledBy;
        this.durationSeconds = durationSeconds;
        this.chatMessages = new ArrayList<>();
        this.votes = new HashMap<>();
    }

    /**
     * Inicia la reunión.
     * En una versión posterior, este método podría activar un temporizador real.
     */
    public void start() {
        System.out.println("La reunión fue convocada por " + calledBy.getUsername() + ".");
        System.out.println("Duración: " + durationSeconds + " segundos.");
    }

    /**
     * Agrega un mensaje de chat a la reunión.
     * 
     * @param msg mensaje enviado por un jugador
     */
    public void addChat(ChatMessage msg) {
        chatMessages.add(msg);
        System.out.println("[Jugador " + msg.getSenderId() + "]: " + msg.getText());
    }

    /**
     * Registra el voto de un jugador hacia un NPC.
     * 
     * @param voterId identificador del jugador que vota
     * @param targetNpcId identificador del NPC al que desea expulsar
     */
    public void castVote(Long voterId, Long targetNpcId) {
        votes.put(voterId, targetNpcId);
        System.out.println("Jugador " + voterId + " votó por el NPC " + targetNpcId + ".");
    }

    /**
     * Cuenta los votos al finalizar la reunión y devuelve el ID del NPC con más votos.
     * Si hay empate, retorna null.
     * 
     * @return identificador del NPC más votado o null si hay empate
     */
    public Long tallyVotes() {
        if (votes.isEmpty()) {
            System.out.println("No se emitieron votos en la reunión.");
            return null;
        }

        Map<Long, Integer> voteCount = new HashMap<>();
        for (Long targetId : votes.values()) {
            voteCount.put(targetId, voteCount.getOrDefault(targetId, 0) + 1);
        }

        Long mostVotedId = null;
        int maxVotes = 0;
        boolean tie = false;

        for (Map.Entry<Long, Integer> entry : voteCount.entrySet()) {
            if (entry.getValue() > maxVotes) {
                mostVotedId = entry.getKey();
                maxVotes = entry.getValue();
                tie = false;
            } else if (entry.getValue() == maxVotes) {
                tie = true;
            }
        }

        if (tie) {
            System.out.println("La votación terminó en empate.");
            return null;
        }

        System.out.println("El NPC más votado fue " + mostVotedId + " con " + maxVotes + " votos.");
        return mostVotedId;
    }
}
