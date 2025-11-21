package com.arsw.shipwreckeds.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Simple health probe consumed by load balancers / orchestration systems.
 * Supports GET and HEAD to avoid 405 responses from AWS ALB health checks.
 */
@RestController
public class HealthController {

    @RequestMapping(value = "/health", method = { RequestMethod.GET, RequestMethod.HEAD })
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
