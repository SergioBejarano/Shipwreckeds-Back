package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.Position;
import org.springframework.stereotype.Service;

/**
 * Service responsible for generating non-player characters with consistent ids
 * and positions.
 */
@Service
public class NpcService {

    private static final long BASE_NPC_ID = 100000L;

    /**
     * Recreates the NPC roster so the number of red avatars matches the number of
     * human castaways.
     *
     * @param match match on which the NPCs will be spawned
     */
    public void generateNpcs(Match match) {
        if (match == null)
            return;
        int humanCount = match.getPlayers() != null ? match.getPlayers().size() : 0;
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
    }
}
