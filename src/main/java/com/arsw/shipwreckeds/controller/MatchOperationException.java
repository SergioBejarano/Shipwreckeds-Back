package com.arsw.shipwreckeds.controller;

import org.springframework.http.HttpStatus;

/**
 * Runtime exception used to signal business rule violations with an associated
 * HTTP status so controllers can translate them without duplicating logic.
 */
public class MatchOperationException extends RuntimeException {

    private final HttpStatus status;

    public MatchOperationException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
