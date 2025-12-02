package com.arsw.shipwreckeds.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.Test;

class CorsPropertiesTest {

    @Test
    void defaultAllowedOriginsIncludePublicHosts() {
        CorsProperties props = new CorsProperties();

        assertTrue(props.getAllowedOrigins().contains("https://shipwreckeds.duckdns.org"));
        assertTrue(props.getAllowedOrigins().contains("https://shipwreckeds-frontend.s3.us-east-1.amazonaws.com"));
    }

    @Test
    void settingNullOrEmptyOriginsRestoresDefaults() {
        CorsProperties props = new CorsProperties();

        props.setAllowedOrigins(Collections.emptyList());

        assertTrue(props.getAllowedOrigins().contains("http://localhost:5173"));
        assertTrue(props.getAllowedOrigins().contains("http://127.0.0.1:5173"));
    }
}
