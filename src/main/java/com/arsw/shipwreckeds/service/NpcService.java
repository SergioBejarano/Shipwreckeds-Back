package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.Position;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service responsible for generating non-player characters with consistent ids
 * and positions.
 */
@Service
public class NpcService {

    public static final long BASE_NPC_ID = 100000L;
    private static final java.util.concurrent.ConcurrentMap<Long, String> INFILTRATOR_ALIAS_BY_PLAYER = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Recreates the NPC roster so the number of red avatars matches the number of
     * human castaways.
     *
     * @param match match on which the NPCs will be spawned
     */
    public void generateNpcs(Match match) {
        if (match == null)
            return;
        List<Player> players = match.getPlayers();
        if (players != null) {
            players.forEach(p -> p.setNpcAlias(null));
        }
        int humanCount = players != null ? players.size() : 0;
        // Ensure red avatars (NPCs + infiltrator) balance the number of blue castaways.
        // With one infiltrator marked,
        // the NPC count becomes (humanCount - 2). If an infiltrator is not yet
        // assigned, the formula resolves to zero.
        int desiredNpcCount = Math.max(0, humanCount - 2);

        match.getNpcs().clear();

        double islandRadius = 100.0; // default radius if not provided by the GameEngine
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < desiredNpcCount; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double r = rnd.nextDouble() * (islandRadius * 0.7); // keep them closer to center
            double x = Math.cos(ang) * r;
            double y = Math.sin(ang) * r;
            long npcId = BASE_NPC_ID + i;
            Npc npc = new Npc(npcId, "npc-skin-" + (i + 2), new Position(x, y), 0.8, false);
            match.addNpc(npc);
        }

        assignInfiltratorAlias(match, desiredNpcCount);
    }

    private void assignInfiltratorAlias(Match match, int generatedCount) {
        Player infiltrator = match.getInfiltrator();
        if (infiltrator == null)
            return;
        String alias = "NPC-" + (BASE_NPC_ID + generatedCount);
        infiltrator.setNpcAlias(alias);
        if (infiltrator.getId() != null) {
            INFILTRATOR_ALIAS_BY_PLAYER.put(infiltrator.getId(), alias);
        }
    }

    public static String lookupAlias(Long playerId) {
        if (playerId == null)
            return null;
        return INFILTRATOR_ALIAS_BY_PLAYER.get(playerId);
    }
}
