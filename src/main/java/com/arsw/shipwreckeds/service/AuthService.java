package com.arsw.shipwreckeds.service;

import com.arsw.shipwreckeds.integration.cognito.CognitoAuthenticationClient;
import com.arsw.shipwreckeds.model.Player;
import com.arsw.shipwreckeds.model.dto.CognitoTokens;
import com.arsw.shipwreckeds.model.dto.LoginResponse;
import com.arsw.shipwreckeds.service.session.PlayerSessionStore;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory authentication service used for the MVP lobby.
 * <p>
 * Players are tracked in memory only; there is no persistent storage or real
 * authentication backend.
 *
 * @author Daniel Ruge
 * @version 19/10/2025
 */
@Service
public class AuthService {

    private final AtomicLong nextId = new AtomicLong(System.currentTimeMillis());
    private final CognitoAuthenticationClient cognitoAuthenticationClient;
    private final PlayerSessionStore sessionStore;

    public AuthService(CognitoAuthenticationClient cognitoAuthenticationClient, PlayerSessionStore sessionStore) {
        this.cognitoAuthenticationClient = cognitoAuthenticationClient;
        this.sessionStore = sessionStore;
    }

    /**
     * Attempts to authenticate the supplied credentials and allocate a new
     * in-memory player session.
     * The login is rejected if the username already has an active session.
     *
     * @param username player identifier attempting to log in
     * @param password plain-text password to validate against the predefined
     *                 accounts
     * @return {@link Player} representing the active session
     * @throws IllegalArgumentException if the credentials are missing, invalid, or
     *                                  the player is already connected
     */
    public LoginResponse login(String username, String password) {
        if (username == null || username.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Por favor ingresa tu nombre y contraseña para continuar.");
        }

        if (sessionStore.hasActiveSession(username)) {
            throw new IllegalArgumentException("Usuario ya conectado desde otro cliente.");
        }

        CognitoTokens tokens = cognitoAuthenticationClient.authenticate(username, password);
        Player registered = registerSession(username, tokens);
        return new LoginResponse(registered, tokens);
    }

    public LoginResponse loginWithAuthorizationCode(String code, String redirectUri) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("El código de autorización es obligatorio.");
        }
        if (!StringUtils.hasText(redirectUri)) {
            throw new IllegalArgumentException("El redirectUri es obligatorio.");
        }

        CognitoTokens tokens = cognitoAuthenticationClient.exchangeAuthorizationCode(code, redirectUri);
        String username = resolveUsername(tokens);
        Player registered = registerSession(username, tokens);
        return new LoginResponse(registered, tokens);
    }

    /**
     * Retrieves the logged-in {@link Player} with the given username, if present.
     *
     * @param username unique player identifier
     * @return player if connected, or {@code null} when no session exists for that
     *         username
     */
    public Player getPlayer(String username) {
        if (!StringUtils.hasText(username))
            return null;
        return sessionStore.getPlayer(username);
    }

    /**
     * Removes the player from the active session registry.
     *
     * @param username identifier of the player to disconnect
     * @return {@code true} if a session was removed, otherwise {@code false}
     */
    public boolean logout(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return sessionStore.deleteSession(username);
    }

    public CognitoTokens getTokens(String username) {
        if (!StringUtils.hasText(username))
            return null;
        return sessionStore.getTokens(username);
    }

    private Player registerSession(String username, CognitoTokens tokens) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("El nombre de usuario es obligatorio.");
        }

        Player candidate = new Player(nextId.getAndIncrement(), username, "default-skin", null);
        long ttlSeconds = resolveTtl(tokens);
        boolean stored = sessionStore.createSession(candidate, tokens, ttlSeconds);
        if (!stored) {
            throw new IllegalArgumentException("Usuario ya conectado desde otro cliente.");
        }
        System.out.println("Jugador conectado: " + username);
        return candidate;
    }

    private long resolveTtl(CognitoTokens tokens) {
        if (tokens != null && tokens.expiresIn() != null && tokens.expiresIn() > 0) {
            return tokens.expiresIn();
        }
        return PlayerSessionStore.DEFAULT_TTL_SECONDS;
    }

    private String resolveUsername(CognitoTokens tokens) {
        if (tokens == null || !StringUtils.hasText(tokens.idToken())) {
            throw new IllegalStateException("Cognito no devolvió un id_token para identificar al usuario.");
        }
        try {
            JWTClaimsSet claims = SignedJWT.parse(tokens.idToken()).getJWTClaimsSet();
            String username = firstNonBlank(
                    claims.getStringClaim("cognito:username"),
                    claims.getStringClaim("username"),
                    claims.getStringClaim("preferred_username"),
                    claims.getSubject());
            if (!StringUtils.hasText(username)) {
                throw new IllegalStateException("No fue posible determinar el usuario autenticado.");
            }
            return username;
        } catch (ParseException e) {
            throw new IllegalStateException("Token de Cognito inválido.", e);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null)
            return null;
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
