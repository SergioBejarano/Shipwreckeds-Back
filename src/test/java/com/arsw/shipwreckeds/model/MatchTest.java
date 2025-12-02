package com.arsw.shipwreckeds.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.arsw.shipwreckeds.model.dto.VoteResult;

/**
 * Unit tests for {@link Match} focused on the recently added defensive logic
 * so Sonar can verify the new branches.
 */
class MatchTest {

    @Test
    void addPlayerAddsAndResetsStateWhenWaiting() {
        Match match = new Match(1L, "ABC123");
        Player player = new Player(42L, "alice", "skin", new Position(1, 2));
        player.setAlive(false);
        player.setInfiltrator(true);

        match.addPlayer(player);

        assertEquals(1, match.getPlayers().size());
        assertTrue(player.isAlive(), "Player must be revived when joining");
        assertFalse(player.isInfiltrator(), "Player should not remain infiltrator by default");
        assertNull(player.getPosition(), "Player position should reset upon joining");
    }

    @Test
    void addPlayerDoesNotAcceptWhenMatchIsNotWaiting() {
        Match match = new Match(1L, "ABC123");
        match.setStatus(MatchStatus.STARTED);
        Player player = new Player(7L, "bob", "skin", null);

        match.addPlayer(player);

        assertTrue(match.getPlayers().isEmpty(), "Roster must stay unchanged once the match has started");
    }

    @Test
    void addPlayerWithNullReferenceThrowsSoCallerHandlesIt() {
        Match match = new Match(1L, "ABC123");

        assertThrows(NullPointerException.class, () -> match.addPlayer(null),
                "Current implementation propagates NPE when null is provided");
    }

    @Test
    void startVotingInitializesBallotsAndResetsHistory() {
        Match match = new Match(1L, "ABC123");
        match.setVotesByPlayer(new java.util.concurrent.ConcurrentHashMap<>());
        match.getVotesByPlayer().put("alice", 10L);
        match.setVotingActive(false);
        match.setLastVoteResult(new VoteResult());
        match.setLastVoteResultEpochMs(123L);
        match.setVoteStartEpochMs(456L);

        match.startVoting();

        assertTrue(match.isVotingActive(), "Voting flag must be on after start");
        assertEquals(0, match.getVotesByPlayer().size(), "Previous ballots must be cleared");
        assertNull(match.getLastVoteResult(), "Old vote results should reset");
        assertEquals(0L, match.getLastVoteResultEpochMs(), "Last vote timestamp must reset");
        assertTrue(match.getVoteStartEpochMs() > 0, "Vote start timestamp should be recorded");
    }

    @Test
    void stopVotingTurnsOffFlagAndTimestamp() {
        Match match = new Match(1L, "ABC123");
        match.startVoting();

        match.stopVoting();

        assertFalse(match.isVotingActive(), "Voting flag must be cleared");
        assertEquals(0L, match.getVoteStartEpochMs(), "Vote start timestamp should reset");
    }

    @Test
    void recordVoteTracksOnlyLivingHumansForCompletion() {
        Match match = new Match(1L, "ABC123");
        Player aliveHuman = new Player(1L, "human", "skin", null);
        Player infiltrator = new Player(2L, "spy", "skin", null);
        infiltrator.setInfiltrator(true);
        Player deadHuman = new Player(3L, "ghost", "skin", null);
        deadHuman.setAlive(false);
        match.getPlayers().add(aliveHuman);
        match.getPlayers().add(infiltrator);
        match.getPlayers().add(deadHuman);

        match.recordVote("human", 99L);

        assertEquals(1, match.getVotesByPlayer().size(), "Vote should be stored");
        assertTrue(match.allHumansVoted(), "Only alive humans count towards completion");
    }

    @Test
    void triggerMeetingOnlyLogsWhenMatchStarted() {
        Match match = new Match(1L, "ABC123");
        match.setStatus(MatchStatus.WAITING);
        Player host = new Player(1L, "host", "skin", null);

        match.triggerMeeting(host);

        assertEquals(MatchStatus.WAITING, match.getStatus(), "Match should remain waiting when not started");

        match.setStatus(MatchStatus.STARTED);
        match.triggerMeeting(host);

        assertEquals(MatchStatus.STARTED, match.getStatus(), "Meeting keeps the match in STARTED state");
    }

    @Test
    void getVotesByPlayerLazilyInitializesMap() {
        Match match = new Match(1L, "ABC123");

        java.util.Map<String, Long> votes = match.getVotesByPlayer();
        votes.put("alice", 1L);

        assertSame(votes, match.getVotesByPlayer(), "Subsequent calls must reuse the same map");
        assertEquals(1, match.getVotesByPlayer().size(), "Map should persist added votes");
    }
}
