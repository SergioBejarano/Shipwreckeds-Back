package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.model.Match;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CreateMatchResponse;
import com.arsw.shipwreckeds.service.MatchService;
import com.arsw.shipwreckeds.service.cache.MatchCacheRepository;
import com.arsw.shipwreckeds.service.cache.MatchLockManager;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para MatchService en su versión respaldada por cache
 * distribuido.
 */
class MatchServiceTest {

    private MatchCacheRepository cacheRepository;
    private MatchLockManager lockManager;
    private MatchService matchService;

    @BeforeEach
    void setUp() {
        cacheRepository = mock(MatchCacheRepository.class);
        lockManager = mock(MatchLockManager.class);
        matchService = new MatchService(cacheRepository, lockManager);

        lenient().when(lockManager.withLock(anyString(), ArgumentMatchers.<Supplier<Object>>any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<Object> supplier = (Supplier<Object>) inv.getArgument(1);
            return supplier.get();
        });
    }

    @Test
    void createMatch_success_generatesCodeAndRegistersMatch() {
        Player host = new Player();
        host.setUsername("hostA");
        when(cacheRepository.findActive(anyString())).thenReturn(null);

        CreateMatchResponse resp = matchService.createMatch(host);

        assertNotNull(resp);
        assertNotNull(resp.getCode());
        assertEquals(6, resp.getCode().length());

        ArgumentCaptor<Match> matchCaptor = ArgumentCaptor.forClass(Match.class);
        verify(cacheRepository).save(matchCaptor.capture(), eq(7200L));
        Match stored = matchCaptor.getValue();
        assertEquals(resp.getCode(), stored.getCode());
        assertEquals("hostA", stored.getPlayers().get(0).getUsername());
    }

    @Test
    void joinMatch_invalidCode_throwsIllegalArgumentException() {
        Player p = new Player();
        p.setUsername("x");

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> matchService.joinMatch(null, p));
        assertEquals("Código inválido.", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> matchService.joinMatch("   ", p));
        assertEquals("Código inválido.", ex2.getMessage());
    }

    @Test
    void joinMatch_notFound_throwsIllegalArgumentException() {
        Player p = new Player();
        p.setUsername("x");
        when(cacheRepository.findActive("NOEX")).thenReturn(null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> matchService.joinMatch("NOEX", p));
        assertEquals("Código inválido o partida no encontrada.", ex.getMessage());
        verify(cacheRepository, never()).save(any(Match.class), anyLong());
    }

    @Test
    void joinMatch_success_addsPlayerToMatch() {
        Match existing = new Match(10L, "JOINME");
        Player host = new Player();
        host.setUsername("host");
        existing.addPlayer(host);
        when(cacheRepository.findActive("JOINME")).thenReturn(existing);

        Player joiner = new Player();
        joiner.setUsername("player1");

        Match returned = matchService.joinMatch("JOINME", joiner);

        assertSame(existing, returned);
        assertEquals(2, returned.getPlayers().size());
        assertTrue(returned.getPlayers().stream().anyMatch(p -> "player1".equals(p.getUsername())));
        verify(cacheRepository).save(existing, 7200L);
    }

    @Test
    void joinMatch_fullMatch_throwsWhenOverCapacity() {
        Match full = new Match(11L, "FULLER");
        IntStream.range(0, 8).forEach(i -> {
            Player pl = new Player();
            pl.setUsername("p" + i);
            full.addPlayer(pl);
        });
        when(cacheRepository.findActive("FULLER")).thenReturn(full);

        Player next = new Player();
        next.setUsername("overflow");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> matchService.joinMatch("FULLER", next));
        assertEquals("La partida está llena.", ex.getMessage());
        verify(cacheRepository, never()).save(full, 7200L);
    }

    @Test
    void joinMatch_duplicateName_throws() {
        Match match = new Match(12L, "DUPL");
        Player first = new Player();
        first.setUsername("sameName");
        match.addPlayer(first);
        when(cacheRepository.findActive("DUPL")).thenReturn(match);

        Player second = new Player();
        second.setUsername("sameName");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> matchService.joinMatch("DUPL", second));
        assertEquals("Ya hay un jugador con ese nombre en la partida.", ex.getMessage());
        verify(cacheRepository, never()).save(match, 7200L);
    }
}
