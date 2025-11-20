package com.arsw.shipwreckeds.integration.cognito;

import com.arsw.shipwreckeds.config.CognitoProperties;
import com.arsw.shipwreckeds.model.dto.CognitoTokens;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Default implementation that calls Cognito's password grant endpoint.
 */
@Component
public class HttpCognitoAuthenticationClient implements CognitoAuthenticationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCognitoAuthenticationClient.class);

    private final CognitoProperties properties;
    private final RestTemplate restTemplate;

    public HttpCognitoAuthenticationClient(CognitoProperties properties, RestTemplateBuilder builder) {
        this.properties = properties;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public CognitoTokens authenticate(String username, String password) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password");
        body.add("client_id", properties.getClientId());
        body.add("username", username);
        body.add("password", password);
        if (properties.getScope() != null && !properties.getScope().isBlank()) {
            body.add("scope", properties.getScope());
        }
        return executeTokenRequest(body, true);
    }

    @Override
    public CognitoTokens exchangeAuthorizationCode(String code, String redirectUri) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("El código de autorización es obligatorio.");
        }
        if (!StringUtils.hasText(redirectUri)) {
            throw new IllegalArgumentException("El redirectUri es obligatorio.");
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", properties.getClientId());
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        return executeTokenRequest(body, false);
    }

    private CognitoTokens executeTokenRequest(MultiValueMap<String, String> body,
            boolean treat401AsInvalidCredentials) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (properties.hasClientSecret()) {
            headers.setBasicAuth(properties.getClientId(), properties.getClientSecret());
        }

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(properties.tokenEndpoint(), entity,
                    TokenResponse.class);
            TokenResponse tokens = response.getBody();
            if (tokens == null || tokens.accessToken == null) {
                throw new IllegalStateException("Cognito respondió sin tokens");
            }
            return tokens.toTokens();
        } catch (HttpStatusCodeException ex) {
            int status = ex.getStatusCode().value();
            if ((status == 400 || status == 401) && treat401AsInvalidCredentials) {
                throw new IllegalArgumentException("Credenciales inválidas.");
            }
            LOGGER.error("Error HTTP al contactar Cognito: {} - {}", status, ex.getResponseBodyAsString());
            throw new IllegalStateException("Falla al contactar Cognito.", ex);
        } catch (ResourceAccessException ex) {
            LOGGER.error("No fue posible contactar Cognito", ex);
            throw new IllegalStateException("No fue posible contactar Cognito.", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("expires_in") Long expiresIn,
            @JsonProperty("token_type") String tokenType) {

        private CognitoTokens toTokens() {
            return new CognitoTokens(accessToken, idToken, refreshToken, expiresIn, tokenType);
        }
    }
}
