package com.arsw.shipwreckeds.model;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * Clase que representa un mensaje dentro del chat de una reunión.
 * 
 * Cada mensaje contiene el identificador del jugador que lo envió,
 * el texto del mensaje y la fecha en que fue enviado. 
 * Esta clase se utiliza para registrar la conversación entre los jugadores
 * durante una reunión en la partida.
 * 
 * @author Daniel Ruge
 * @version 19/10/2025
 */
@Getter
@Setter
public class ChatMessage {

    // Atributos principales
    private Long id;
    private Long senderId;
    private String text;
    private Date timestamp;

    /**
     * Constructor principal para crear un mensaje de chat.
     * 
     * @param id identificador único del mensaje
     * @param senderId identificador del jugador que envió el mensaje
     * @param text contenido del mensaje
     * @param timestamp momento en que fue enviado
     */
    public ChatMessage(Long id, Long senderId, String text, Date timestamp) {
        this.id = id;
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
    }

    /**
     * Muestra el mensaje en formato legible para depuración o registro.
     * 
     * @return texto con el formato [hora] (ID del remitente): mensaje
     */
    @Override
    public String toString() {
        return "[" + timestamp + "] (Jugador " + senderId + "): " + text;
    }
}
