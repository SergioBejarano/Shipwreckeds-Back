package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Player;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service in charge of selecting the human infiltrator for each match.
 */
@Service
public class RoleService {

    /**
     * Randomly assigns one human player as infiltrator and resets the rest to
     * regular castaways.
     *
     * @param match match whose player roster will be updated
     */
    public void assignHumanRoles(Match match) {
        List<Player> players = match.getPlayers();
        if (players == null || players.isEmpty())
            return;
        java.util.Collections.shuffle(players);
        // asignar primer jugador aleatorio como infiltrado humano
        Player infiltrator = players.get(0);
        for (Player p : players) {
            p.setInfiltrator(false);
        }
        infiltrator.setInfiltrator(true);
        match.setInfiltrator(infiltrator);
    }
}
