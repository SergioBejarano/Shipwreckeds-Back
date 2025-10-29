package com.arsw.shipwreckeds.model;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a single entry in the meeting chat log.
 * <p>
 * Each message stores the identifier of the player who sent it, the textual
 * payload, and the timestamp of emission.
 * The server relies on this model to reconstruct the conversation history
 * during a meeting.
 *
 */
@Getter
@Setter
public class ChatMessage {

    private Long id;
    private Long senderId;
    private String text;
    private Date timestamp;

    /**
     * Creates an immutable chat message representation.
     *
     * @param id        unique identifier assigned by the server
     * @param senderId  identifier of the player that authored the message
     * @param text      message content
     * @param timestamp time at which the message was emitted
     */
    public ChatMessage(Long id, Long senderId, String text, Date timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    /**
     * Formats the message for logging purposes.
     *
     * @return string in the form {@code [timestamp] (Jugador senderId): text}
     */
    @Override
    public String toString() {
        return "[" + timestamp + "] (Jugador " + senderId + "): " + text;
    }
}
