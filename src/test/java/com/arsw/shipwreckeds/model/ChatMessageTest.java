package com.arsw.shipwreckeds.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.Test;

class ChatMessageTest {

    @Test
    void toStringIncludesSenderAndText() {
        Date timestamp = new Date(0L);
        ChatMessage message = new ChatMessage(1L, 7L, "SOS", timestamp);

        String printable = message.toString();

        assertTrue(printable.contains("(Jugador 7)"));
        assertTrue(printable.endsWith(": SOS"));
        assertTrue(printable.startsWith("["), "Representation should begin with a timestamp");
    }
}
