package com.arsw.shipwreckeds.websocket;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Payload envelope used to replicate WebSocket/STOMP events across all backend
 * instances via Redis Pub/Sub.
 */
public record DistributedWsEvent(String sourceId, String destination, JsonNode payload) {
}
