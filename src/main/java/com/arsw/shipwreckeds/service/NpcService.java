package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Npc;
import com.arsw.shipwreckeds.model.Position;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class NpcService {

    // start NPC ids at a high offset to avoid colliding with player ids
    private final AtomicLong nextNpcId = new AtomicLong(100000);

    public void generateNpcs(Match match) {
        if (match == null)
            return;
        int humanCount = match.getPlayers() != null ? match.getPlayers().size() : 0;
        // Queremos que la cantidad total de "rojos" (NPCs + infiltrado humano) iguale
        // la cantidad de náufragos (jugadores azules). Dado que ya existe exactamente
        // un
        // infiltrado humano marcado en el match, la cantidad de NPCs debe ser
        // (humanCount - 2). Si por alguna razón no hay infiltrado asignado todavía,
        // esta fórmula mantiene al menos 0 NPCs.
        int desiredNpcCount = Math.max(0, humanCount - 2);

        match.getNpcs().clear();

        double islandRadius = 100.0; // default radius if not provided by the GameEngine
        java.util.Random rnd = new java.util.Random();
        for (int i = 0; i < desiredNpcCount; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double r = rnd.nextDouble() * (islandRadius * 0.7); // keep them closer to center
            double x = Math.cos(ang) * r;
            double y = Math.sin(ang) * r;
            Npc npc = new Npc(nextNpcId.getAndIncrement(), "npc-skin-" + (i + 2), new Position(x, y), 0.8, false);
            match.addNpc(npc);
        }
    }
}
