package com.arsw.shipwreckeds;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@SpringBootTest
@ActiveProfiles("test")
class ShipwreckedsBackendApplicationTests {

    // Mock de Redis para que no intente conectarse al servidor real
    // Mock de Redis para que no intente conectarse al servidor real
    @MockBean(name = "matchRedisTemplate")
    private RedisTemplate<?, ?> matchRedisTemplate;

    @MockBean(name = "playerSessionRedisTemplate")
    private RedisTemplate<?, ?> playerSessionRedisTemplate;

    @MockBean(name = "stringRedisTemplate")
    private StringRedisTemplate stringRedisTemplate;

    // Provide a mock RedisConnectionFactory so configuration beans that require it can initialize
    @MockBean
    private RedisConnectionFactory redisConnectionFactory;

    // Prevent the actual RedisMessageListenerContainer from starting during tests
    @MockBean
    private RedisMessageListenerContainer redisMessageListenerContainer;

    // Mock de WebSocket messaging
    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    // Aquí se agregan otros beans que causen errores al levantar el contexto
    // @MockBean
    // private OtraDependencia externa;

    @Test
    void contextLoads() {
        // Solo valida que Spring Boot pueda levantar el ApplicationContext
    }
}
