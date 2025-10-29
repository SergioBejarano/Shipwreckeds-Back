package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para RoleService.
 *
 *
 * @author Daniel Ruge
 * @version 2025-10-29
 */
class RoleServiceTest {

    private RoleService roleService;

    @BeforeEach
    void setUp() {
        roleService = new RoleService();
    }

    @Test
    void assignHumanRoles_assignsExactlyOneInfiltrator() {
        Match match = new Match(1L, "ABC123");
        Player p1 = new Player(1L, "ana", "default", null);
        Player p2 = new Player(2L, "bruno", "default", null);
        Player p3 = new Player(3L, "carla", "default", null);
        match.addPlayer(p1);
        match.addPlayer(p2);
        match.addPlayer(p3);

        roleService.assignHumanRoles(match);

        List<Player> players = match.getPlayers();
        long infiltratorCount = players.stream().filter(Player::isInfiltrator).count();
        assertEquals(1, infiltratorCount, "Debe haber exactamente un infiltrador");

        // Comprobar que match tiene el infiltrador asignado
        assertNotNull(match.getInfiltrator(), "Match debe registrar el infiltrador");
        assertTrue(match.getInfiltrator().isInfiltrator(), "El infiltrador registrado debe estar marcado como infiltrador");
    }

    @Test
    void assignHumanRoles_resetsOtherPlayers() {
        Match match = new Match(2L, "DEF456");
        Player p1 = new Player(1L, "diego", "default", null);
        Player p2 = new Player(2L, "eva", "default", null);
        match.addPlayer(p1);
        match.addPlayer(p2);

        roleService.assignHumanRoles(match);

        for (Player p : match.getPlayers()) {
            if (p.equals(match.getInfiltrator())) {
                assertTrue(p.isInfiltrator(), "El infiltrador debe estar marcado como infiltrador");
            } else {
                assertFalse(p.isInfiltrator(), "Todos los demás jugadores no deben ser infiltradores");
            }
        }
    }

    @Test
    void assignHumanRoles_emptyOrNullPlayers_doesNotThrow() {
        Match match1 = new Match(3L, "GHI789");
        roleService.assignHumanRoles(match1); // lista vacía
        assertNull(match1.getInfiltrator(), "Match sin jugadores no debe tener infiltrador");

        Match match2 = new Match(4L, "JKL012") {
            @Override
            public List<Player> getPlayers() {
                return null; // simulamos null
            }
        };
        assertDoesNotThrow(() -> roleService.assignHumanRoles(match2), "No debe lanzar excepción si la lista es null");
        assertNull(match2.getInfiltrator(), "Match con lista null no debe tener infiltrador");
    }
}
