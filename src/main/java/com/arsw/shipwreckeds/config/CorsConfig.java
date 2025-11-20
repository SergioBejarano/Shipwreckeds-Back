package com.arsw.shipwreckeds.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralizes the CORS configuration for every REST endpoint.
 */
@Configuration
@EnableConfigurationProperties(CorsProperties.class)
public class CorsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CorsConfig.class);

    private final CorsProperties properties;

    public CorsConfig(CorsProperties properties) {
        this.properties = properties;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(new java.util.ArrayList<>(properties.getAllowedOrigins()));
        config.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(
                java.util.List.of("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(java.util.List.of("Authorization", "Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(properties.getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        LOGGER.info("CORS enabled for origins: {}", properties.getAllowedOrigins());
        return source;
    }

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter(
            @Qualifier("corsConfigurationSource") CorsConfigurationSource source) {
        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
