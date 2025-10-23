package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.Position;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class NpcService {

    private final AtomicLong nextNpcId = new AtomicLong(1);

    /**
     * Genera NPCs simples para la partida. Según la épica se crean 3 NPCs:
     * - 1 NPC infiltrado
     * - 2 o 3 NPCs náufragos (aquí generamos 3 en total: 1 infiltrado + 2
     * náufragos)
     */
    public void generateNpcs(Match match) {
        if (match == null)
            return;
        // generate 3 NPCs with random positions inside default island radius
        double islandRadius = 100.0; // default radius if not provided by a GameEngine
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < 3; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double r = rnd.nextDouble() * (islandRadius * 0.7); // keep them closer to center
            double x = Math.cos(ang) * r;
            double y = Math.sin(ang) * r;
            Npc npc = new Npc(nextNpcId.getAndIncrement(), "npc-skin-" + (i + 2), new Position(x, y), 0.8, false);
            match.addNpc(npc);
        }
    }
}
