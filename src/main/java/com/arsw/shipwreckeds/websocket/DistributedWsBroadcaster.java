package com.arsw.shipwreckeds.websocket;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Bridges the in-memory Spring WebSocket broker with Redis Pub/Sub so that all
 * backend instances
 * broadcast the same STOMP events regardless of which node executed the game
 * logic.
 */
@Component
public class DistributedWsBroadcaster implements MessageListener {

    public static final String WS_CHANNEL = "shipwreckeds:ws:events";

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedWsBroadcaster.class);

    private final String instanceId = UUID.randomUUID().toString();
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public DistributedWsBroadcaster(StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Publishes the given payload so that every instance (including the current
     * one)
     * relays it to its connected WebSocket sessions.
     */
    public void publish(String destination, Object payload) {
        if (destination == null || payload == null) {
            return;
        }
        try {
            JsonNode payloadNode = objectMapper.valueToTree(payload);
            DistributedWsEvent event = new DistributedWsEvent(instanceId, destination, payloadNode);
            redisTemplate.convertAndSend(WS_CHANNEL, objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize WebSocket payload for destination {}", destination, e);
        } catch (Exception e) {
            LOGGER.error("Failed to publish WebSocket event to Redis channel {}", WS_CHANNEL, e);
        }
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        if (message == null || message.getBody() == null) {
            return;
        }
        try {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            DistributedWsEvent event = objectMapper.readValue(body, DistributedWsEvent.class);
            messagingTemplate.convertAndSend(event.destination(), event.payload());
        } catch (Exception e) {
            LOGGER.error("Failed to dispatch distributed WebSocket event", e);
        }
    }
}
