package com.arsw.shipwreckeds.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DistributedWsBroadcasterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private ObjectMapper objectMapper;
    private DistributedWsBroadcaster broadcaster;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        broadcaster = new DistributedWsBroadcaster(redisTemplate, objectMapper, messagingTemplate);
    }

    @Test
    void publishSerializesPayloadAndSendsItToRedis() throws Exception {
        Map<String, Object> payload = Map.of("message", "hello");

        broadcaster.publish("/topic/game/ABC", payload);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisTemplate).convertAndSend(eq(DistributedWsBroadcaster.WS_CHANNEL), jsonCaptor.capture());
        DistributedWsEvent event = objectMapper.readValue(jsonCaptor.getValue(), DistributedWsEvent.class);
        assertEquals("/topic/game/ABC", event.destination());
        assertEquals("hello", event.payload().get("message").asText());
    }

    @Test
    void publishIgnoresNullArguments() {
        broadcaster.publish(null, new Object());
        broadcaster.publish("/topic/game/XYZ", null);

        verifyNoInteractions(redisTemplate);
    }

    @Test
    void onMessageDispatchesPayload() throws Exception {
        JsonNode payload = objectMapper.valueToTree(Map.of("foo", "bar"));
        DistributedWsEvent event = new DistributedWsEvent("instance", "/topic/foo", payload);
        String body = objectMapper.writeValueAsString(event);
        var message = mock(org.springframework.data.redis.connection.Message.class);
        when(message.getBody()).thenReturn(body.getBytes(StandardCharsets.UTF_8));

        broadcaster.onMessage(message, null);

        verify(messagingTemplate).convertAndSend("/topic/foo", payload);
    }

    @Test
    void onMessageWithEmptyBodyDoesNothing() {
        var message = mock(org.springframework.data.redis.connection.Message.class);
        when(message.getBody()).thenReturn(null);

        broadcaster.onMessage(message, null);

        verifyNoInteractions(messagingTemplate);
    }
}
