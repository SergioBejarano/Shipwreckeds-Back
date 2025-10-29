package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CreateMatchResponse;
import com.arsw.shipwreckeds.service.MatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para MatchService.
 *
 *
 * @author Daniel Ruge
 * @version 2025-10-29
 */
class MatchServiceTest {

    private MatchService matchService;

    @BeforeEach
    void setUp() {
        matchService = new MatchService();
    }

    @Test
    void createMatch_success_generatesCodeAndRegistersMatch() {
        Player host = mock(Player.class);
        when(host.getUsername()).thenReturn("hostA");

        CreateMatchResponse resp = matchService.createMatch(host);
        assertNotNull(resp);
        String code = resp.getCode();
        assertNotNull(code);
        assertEquals(6, code.length(), "El código debe tener longitud " + 6);

        Match stored = matchService.getMatchByCode(code);
        assertNotNull(stored, "La partida creada debería estar disponible por código");
        // the host should have been added to the match
        assertTrue(stored.getPlayers().stream().anyMatch(p -> p == host || p.getUsername().equals("hostA")));
    }

    @Test
    void joinMatch_success_addsPlayerToMatch() {
        Player host = mock(Player.class);
        when(host.getUsername()).thenReturn("hostB");
        CreateMatchResponse resp = matchService.createMatch(host);
        String code = resp.getCode();

        Player joiner = mock(Player.class);
        when(joiner.getUsername()).thenReturn("player1");

        Match returned = matchService.joinMatch(code, joiner);
        assertNotNull(returned);
        assertTrue(returned.getPlayers().stream().anyMatch(p -> p == joiner || p.getUsername().equals("player1")));
    }

    @Test
    void joinMatch_invalidCode_throwsIllegalArgumentException() {
        Player p = mock(Player.class);
        when(p.getUsername()).thenReturn("x");

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> matchService.joinMatch(null, p));
        assertEquals("Código inválido.", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> matchService.joinMatch("   ", p));
        assertEquals("Código inválido.", ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> matchService.joinMatch("NOEX", p));
        assertEquals("Código inválido o partida no encontrada.", ex3.getMessage());
    }

    @Test
    void joinMatch_expired_throwsAndRemovesFromRegistry() throws Exception {
        Player host = mock(Player.class);
        when(host.getUsername()).thenReturn("hostC");
        CreateMatchResponse resp = matchService.createMatch(host);
        String code = resp.getCode();

        // Use reflection to set stored.createdAtEpochSec to far past so it is expired
        Field matchesField = MatchService.class.getDeclaredField("matchesByCode");
        matchesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) matchesField.get(matchService);
        Object storedMatch = map.get(code);

        // storedMatch is an instance of the private static inner class StoredMatch
        Class<?> storedCls = storedMatch.getClass();
        Field createdAtField = storedCls.getDeclaredField("createdAtEpochSec");
        createdAtField.setAccessible(true);
        Field ttlField = storedCls.getDeclaredField("ttlSeconds");
        ttlField.setAccessible(true);
        long ttl = (long) ttlField.get(storedMatch);

        long past = Instant.now().getEpochSecond() - (ttl + 10);
        createdAtField.set(storedMatch, past);

        Player joiner = mock(Player.class);
        when(joiner.getUsername()).thenReturn("playerX");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> matchService.joinMatch(code, joiner));
        assertEquals("El código ha caducado.", ex.getMessage());

        // after expiration, getMatchByCode should return null and the entry removed
        assertNull(matchService.getMatchByCode(code), "La entrada expirada debería haberse eliminado");
        assertFalse(map.containsKey(code), "El mapa interno no debe contener el código después de caducar");
    }

    @Test
    void getMatchByCode_expired_prunesAndReturnsNull() throws Exception {
        Player host = mock(Player.class);
        when(host.getUsername()).thenReturn("hostD");
        CreateMatchResponse resp = matchService.createMatch(host);
        String code = resp.getCode();

        // expire it similarly
        Field matchesField = MatchService.class.getDeclaredField("matchesByCode");
        matchesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) matchesField.get(matchService);
        Object storedMatch = map.get(code);
        Class<?> storedCls = storedMatch.getClass();
        Field createdAtField = storedCls.getDeclaredField("createdAtEpochSec");
        createdAtField.setAccessible(true);
        Field ttlField = storedCls.getDeclaredField("ttlSeconds");
        ttlField.setAccessible(true);
        long ttl = (long) ttlField.get(storedMatch);
        long past = Instant.now().getEpochSecond() - (ttl + 5);
        createdAtField.set(storedMatch, past);

        // getMatchByCode should prune and return null
        assertNull(matchService.getMatchByCode(code));
        assertFalse(map.containsKey(code));
    }

    @Test
    void joinMatch_fullMatch_throwsWhenOverCapacity() {
        Player host = mock(Player.class);
        when(host.getUsername()).thenReturn("hostE");
        CreateMatchResponse resp = matchService.createMatch(host);
        String code = resp.getCode();

        // add 7 more players to reach 8 total (host + 7 = 8)
        for (int i = 0; i < 7; i++) {
            Player p = mock(Player.class);
            when(p.getUsername()).thenReturn("p" + i);
            matchService.joinMatch(code, p);
        }

        Player next = mock(Player.class);
        when(next.getUsername()).thenReturn("overflow");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> matchService.joinMatch(code, next));
        assertEquals("La partida está llena.", ex.getMessage());
    }

    @Test
    void joinMatch_duplicateName_throws() {
        Player host = mock(Player.class);
        when(host.getUsername()).thenReturn("hostF");
        CreateMatchResponse resp = matchService.createMatch(host);
        String code = resp.getCode();

        Player p1 = mock(Player.class);
        when(p1.getUsername()).thenReturn("sameName");
        matchService.joinMatch(code, p1);

        Player p2 = mock(Player.class);
        when(p2.getUsername()).thenReturn("sameName");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> matchService.joinMatch(code, p2));
        assertEquals("Ya hay un jugador con ese nombre en la partida.", ex.getMessage());
    }
}
