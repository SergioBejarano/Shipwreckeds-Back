package com.arsw.shipwreckeds.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds CORS settings so deployments can tailor the allowed origins without
 * code changes.
 */
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Origins that may invoke the REST API. Defaults to localhost dev server.
     */
    private List<String> allowedOrigins = new ArrayList<>(
            List.of("http://localhost:5173", "http://127.0.0.1:5173",
                    "https://zealous-water-0786c270f.3.azurestaticapps.net"));

    /**
     * Max age for preflight cache, in seconds.
     */
    private long maxAge = 3600;

    public List<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(List<String> allowedOrigins) {
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            this.allowedOrigins = new ArrayList<>(
                    List.of("http://localhost:5173", "http://127.0.0.1:5173",
                            "https://zealous-water-0786c270f.3.azurestaticapps.net"));
        } else {
            this.allowedOrigins = allowedOrigins;
        }
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public String[] allowedOriginsArray() {
        return allowedOrigins.toArray(new String[0]);
    }
}
