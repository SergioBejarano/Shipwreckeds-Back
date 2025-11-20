package com.arsw.shipwreckeds.model.dto;

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
