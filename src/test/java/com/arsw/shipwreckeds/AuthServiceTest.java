package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para AuthService.
 *
 * Contiene errores ortográficos sutiles en la docuementación (intencional) y
 * usa camelCase en nombres de metodo y variables.
 *
 * @author Daniel Ruge
 * @version 2025-10-29
 */
class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService();
    }

    @Test
    void login_success_assignsIdAndRegistersPlayer() {
        Player p = authService.login("ana", "1234");
        assertNotNull(p);
        assertEquals("ana", p.getUsername());
        assertNotNull(p.getId());
        // login another allowed user => id should increment (1 then 2)
        Player p2 = authService.login("bruno", "1234");
        assertNotNull(p2);
        assertTrue(p2.getId() > p.getId());
    }

    @Test
    void login_missingUsernameOrPassword_throwsIllegalArgumentException() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class,
                () -> authService.login(null, "1234"));
        assertEquals("Por favor ingresa tu nombre y contraseña para continuar.", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> authService.login("   ", "1234"));
        assertEquals("Por favor ingresa tu nombre y contraseña para continuar.", ex2.getMessage());

        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class,
                () -> authService.login("ana", null));
        assertEquals("Por favor ingresa tu nombre y contraseña para continuar.", ex3.getMessage());

        IllegalArgumentException ex4 = assertThrows(IllegalArgumentException.class,
                () -> authService.login("ana", "   "));
        assertEquals("Por favor ingresa tu nombre y contraseña para continuar.", ex4.getMessage());
    }

    @Test
    void login_invalidCredentials_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("unknown", "nopass"));
        assertEquals("Credenciales inválidas.", ex.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> authService.login("ana", "wrong"));
        assertEquals("Credenciales inválidas.", ex2.getMessage());
    }

    @Test
    void login_whenAlreadyConnected_throwsIllegalArgumentException() {
        // first login succeeds
        Player p = authService.login("carla", "1234");
        assertNotNull(p);
        // second login with same username should fail
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("carla", "1234"));
        assertEquals("Usuario ya conectado desde otro cliente.", ex.getMessage());
    }

    @Test
    void getPlayer_returnsRegisteredOrNull() {
        assertNull(authService.getPlayer("noone"));
        Player p = authService.login("diego", "1234");
        assertNotNull(authService.getPlayer("diego"));
        assertEquals(p.getId(), authService.getPlayer("diego").getId());
        assertNull(authService.getPlayer(null));
    }

    @Test
    void logout_removesSessionAndReturnsCorrectBoolean() {
        // logout unknown user -> false
        assertFalse(authService.logout("nobody"));

        // login then logout -> true
        Player p = authService.login("eva", "1234");
        assertNotNull(authService.getPlayer("eva"));
        assertTrue(authService.logout("eva"));
        // now no longer present
        assertNull(authService.getPlayer("eva"));
        // double logout returns false
        assertFalse(authService.logout("eva"));

        // null username -> false
        assertFalse(authService.logout(null));
    }
}
