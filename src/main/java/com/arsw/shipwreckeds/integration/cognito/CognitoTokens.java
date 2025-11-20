package com.arsw.shipwreckeds.integration.cognito;

/**
 * Holder for the tokens returned by Cognito after a successful password login.
 */
public record CognitoTokens(
        String accessToken,
        String idToken,
        String refreshToken,
        Long expiresIn,
        String tokenType) {
}
