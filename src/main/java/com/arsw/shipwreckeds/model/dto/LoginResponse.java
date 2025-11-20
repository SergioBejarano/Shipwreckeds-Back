package com.arsw.shipwreckeds.model.dto;

import com.arsw.shipwreckeds.model.Player;

/**
 * DTO returned after validating credentials with Cognito.
 */
public record LoginResponse(Player player, CognitoTokens tokens) {
}
