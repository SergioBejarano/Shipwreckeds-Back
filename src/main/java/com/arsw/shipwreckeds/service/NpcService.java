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

        // crear 1 NPC infiltrado
        // crear 3 NPCs náufragos adicionales (no se marca ningún NPC como infiltrador
        // aquí)
        Npc npc1 = new Npc(nextNpcId.getAndIncrement(), "npc-skin-2", new Position(1, 1), 0.8, false);
        Npc npc2 = new Npc(nextNpcId.getAndIncrement(), "npc-skin-3", new Position(2, 2), 0.8, false);
        Npc npc3 = new Npc(nextNpcId.getAndIncrement(), "npc-skin-4", new Position(3, 3), 0.8, false);
        match.addNpc(npc1);
        match.addNpc(npc2);
        match.addNpc(npc3);
    }
}
