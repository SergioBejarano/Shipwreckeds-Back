package com.arsw.shipwreckeds;

import com.arsw.shipwreckeds.integration.cognito.CognitoAuthenticationClient;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CognitoTokens;
import com.arsw.shipwreckeds.model.dto.LoginResponse;
import com.arsw.shipwreckeds.service.AuthService;
import com.arsw.shipwreckeds.service.session.PlayerSessionPayload;
import com.arsw.shipwreckeds.service.session.PlayerSessionStore;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pruebas unitarias para AuthService.
 *
 * Contiene errores ortográficos sutiles en la docuementación (intencional) y
 * usa camelCase en nombres de metodo y variables.
 *
 * @author Daniel Ruge
 * @version 2025-10-29
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final byte[] TEST_SIGNING_KEY = "shipwreckeds-test-signing-key-012345"
            .getBytes(java.nio.charset.StandardCharsets.UTF_8);

    @Mock
    private CognitoAuthenticationClient cognitoClient;

    private AuthService authService;
    private InMemoryPlayerSessionStore sessionStore;

    private CognitoTokens tokens;

    @BeforeEach
    void setUp() {
        tokens = tokensFor("ana");
        sessionStore = new InMemoryPlayerSessionStore();
        authService = new AuthService(cognitoClient, sessionStore);
    }

    @Test
    void login_success_assignsIdAndRegistersPlayer() {
        when(cognitoClient.authenticate("ana", "1234")).thenReturn(tokensFor("ana"));
        when(cognitoClient.authenticate("bruno", "1234")).thenReturn(tokensFor("bruno"));

        LoginResponse r1 = authService.login("ana", "1234");
        assertNotNull(r1);
        Player p = r1.player();
        assertEquals("ana", p.getUsername());
        assertNotNull(p.getId());
        assertNotNull(r1.tokens());

        LoginResponse r2 = authService.login("bruno", "1234");
        assertNotNull(r2.player());
        assertTrue(r2.player().getId() > p.getId());
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
        when(cognitoClient.authenticate("unknown", "nopass"))
                .thenThrow(new IllegalArgumentException("Credenciales inválidas."));
        when(cognitoClient.authenticate("ana", "wrong"))
                .thenThrow(new IllegalArgumentException("Credenciales inválidas."));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("unknown", "nopass"));
        assertEquals("Credenciales inválidas.", ex.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class,
                () -> authService.login("ana", "wrong"));
        assertEquals("Credenciales inválidas.", ex2.getMessage());
    }

    @Test
    void login_whenAlreadyConnected_throwsIllegalArgumentException() {
        when(cognitoClient.authenticate("carla", "1234")).thenReturn(tokensFor("carla"));

        // first login succeeds
        LoginResponse r = authService.login("carla", "1234");
        assertNotNull(r);

        // second login with same username should fail before calling Cognito again
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.login("carla", "1234"));
        assertEquals("Usuario ya conectado desde otro cliente.", ex.getMessage());

        verify(cognitoClient, times(1)).authenticate("carla", "1234");
    }

    @Test
    void getPlayer_returnsRegisteredOrNull() {
        when(cognitoClient.authenticate("diego", "1234")).thenReturn(tokensFor("diego"));

        assertNull(authService.getPlayer("noone"));
        LoginResponse resp = authService.login("diego", "1234");
        assertNotNull(authService.getPlayer("diego"));
        assertEquals(resp.player().getId(), authService.getPlayer("diego").getId());
        assertNull(authService.getPlayer(null));
    }

    @Test
    void logout_removesSessionAndReturnsCorrectBoolean() {
        when(cognitoClient.authenticate("eva", "1234")).thenReturn(tokensFor("eva"));

        // logout unknown user -> false
        assertFalse(authService.logout("nobody"));

        // login then logout -> true
        authService.login("eva", "1234");
        assertNotNull(authService.getPlayer("eva"));
        assertTrue(authService.logout("eva"));
        // now no longer present
        assertNull(authService.getPlayer("eva"));
        // double logout returns false
        assertFalse(authService.logout("eva"));

        // null username -> false
        assertFalse(authService.logout(null));
    }

    @Test
    void getTokens_returnsTokensIfPresent() {
        when(cognitoClient.authenticate("ana", "1234")).thenReturn(tokens);
        assertNull(authService.getTokens("ana"));
        authService.login("ana", "1234");
        assertNotNull(authService.getTokens("ana"));
        authService.logout("ana");
        assertNull(authService.getTokens("ana"));
    }

    @Test
    void loginWithAuthorizationCode_usesExchangeAndRegistersPlayer() {
        CognitoTokens codeTokens = tokensFor("diego");
        when(cognitoClient.exchangeAuthorizationCode("abc", "http://localhost"))
                .thenReturn(codeTokens);

        LoginResponse response = authService.loginWithAuthorizationCode("abc", "http://localhost");
        assertEquals("diego", response.player().getUsername());
        verify(cognitoClient).exchangeAuthorizationCode("abc", "http://localhost");
    }

    private CognitoTokens tokensFor(String username) {
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(username)
                    .claim("cognito:username", username)
                    .build();
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(new MACSigner(TEST_SIGNING_KEY));
            return new CognitoTokens("access-" + username, signedJWT.serialize(), "refresh", 3600L, "Bearer");
        } catch (JOSEException e) {
            throw new RuntimeException("No fue posible firmar el token de prueba", e);
        }
    }

    private static class InMemoryPlayerSessionStore implements PlayerSessionStore {

        private final Map<String, PlayerSessionPayload> sessions = new ConcurrentHashMap<>();

        @Override
        public boolean hasActiveSession(String username) {
            return getPayload(username) != null;
        }

        @Override
        public boolean createSession(Player player, CognitoTokens tokens, long ttlSeconds) {
            PlayerSessionPayload payload = new PlayerSessionPayload(player, tokens,
                    System.currentTimeMillis() / 1000, ttlSeconds);
            return sessions.putIfAbsent(player.getUsername(), payload) == null;
        }

        @Override
        public Player getPlayer(String username) {
            PlayerSessionPayload payload = getPayload(username);
            return payload != null ? payload.getPlayer() : null;
        }

        @Override
        public CognitoTokens getTokens(String username) {
            PlayerSessionPayload payload = getPayload(username);
            return payload != null ? payload.getTokens() : null;
        }

        @Override
        public boolean deleteSession(String username) {
            if (username == null) {
                return false;
            }
            return sessions.remove(username) != null;
        }

        private PlayerSessionPayload getPayload(String username) {
            if (username == null) {
                return null;
            }
            PlayerSessionPayload payload = sessions.get(username);
            if (payload == null) {
                return null;
            }
            if (payload.isExpired()) {
                sessions.remove(username);
                return null;
            }
            return payload;
        }
    }
}
