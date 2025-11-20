package com.arsw.shipwreckeds.integration.cognito;

import com.arsw.shipwreckeds.model.dto.CognitoTokens;

/**
 * Minimal abstraction that hides the HTTP interaction with AWS Cognito.
 */
public interface CognitoAuthenticationClient {

    CognitoTokens authenticate(String username, String password);

    CognitoTokens exchangeAuthorizationCode(String code, String redirectUri);
}
