package com.arsw.shipwreckeds.model;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.Test;

class MeetingTest {

    @Test
    void addChatAndVotesAreTracked() {
        Player host = new Player(1L, "host", "skin", new Position(0, 0));
        Meeting meeting = new Meeting(1L, host, 30);
        ChatMessage message = new ChatMessage(1L, host.getId(), "Hola", new Date());

        meeting.addChat(message);
        meeting.castVote(1L, 9L);
        meeting.castVote(2L, 9L);
        meeting.castVote(3L, 8L);

        assertEquals(1, meeting.getChatMessages().size());
        assertEquals(3, meeting.getVotes().size());
        assertEquals(9L, meeting.tallyVotes());
    }

    @Test
    void tallyVotesReturnsNullWhenNoVotesOrTie() {
        Player host = new Player(2L, "host", "skin", new Position(0, 0));
        Meeting meeting = new Meeting(2L, host, 30);

        assertNull(meeting.tallyVotes(), "Empty vote list should yield null");

        meeting.castVote(1L, 9L);
        meeting.castVote(2L, 10L);

        assertNull(meeting.tallyVotes(), "Tie between NPCs should return null");
    }
}
