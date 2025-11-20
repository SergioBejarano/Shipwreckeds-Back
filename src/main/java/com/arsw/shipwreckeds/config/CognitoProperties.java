package com.arsw.shipwreckeds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Simple holder for the Cognito credentials used by the authentication flow.
 */
@Component
@ConfigurationProperties(prefix = "cognito")
public class CognitoProperties {

    /**
     * Hosted UI domain for the user pool. Defaults to the domain shared by the
     * team.
     */
    private String domain = "https://us-east-1symlrgxi6.auth.us-east-1.amazoncognito.com";

    /**
     * Public client id enabled for the Resource Owner Password credentials flow.
     */
    private String clientId = "demo-client-id";

    /**
     * Optional client secret. Leave empty when the client is configured without
     * secret.
     */
    private String clientSecret;

    /**
     * Scope requested during the password grant. Includes the AWS admin scope so
     * that
     * password logins are allowed by Cognito.
     */
    private String scope = "email openid phone";

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String tokenEndpoint() {
        if (!StringUtils.hasText(domain)) {
            throw new IllegalStateException("El dominio de Cognito no está configurado.");
        }
        return domain.endsWith("/") ? domain + "oauth2/token" : domain + "/oauth2/token";
    }

    public boolean hasClientSecret() {
        return StringUtils.hasText(clientSecret);
    }
}
