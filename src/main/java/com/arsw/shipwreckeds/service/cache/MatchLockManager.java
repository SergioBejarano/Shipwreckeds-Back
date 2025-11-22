package com.arsw.shipwreckeds.service.cache;

import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

/**
 * Provides distributed mutual exclusion on top of Redis so that only one
 * backend instance modifies a match at a time.
 */
@Component
public class MatchLockManager {

    private final RedisLockRegistry lockRegistry;

    public MatchLockManager(RedisLockRegistry lockRegistry) {
        this.lockRegistry = lockRegistry;
    }

    public <T> T withLock(String matchCode, Supplier<T> action) {
        return withLock(matchCode, 5, action);
    }

    public <T> T withLock(String matchCode, int waitSeconds, Supplier<T> action) {
        if (matchCode == null || matchCode.isBlank()) {
            return null;
        }
        Lock lock = lockRegistry.obtain(matchCode);
        boolean acquired = false;
        try {
            acquired = lock.tryLock(waitSeconds, TimeUnit.SECONDS);
            if (!acquired) {
                throw new IllegalStateException("No se pudo obtener el lock distribuido para la partida " + matchCode);
            }
            return action.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupción al obtener lock para partida " + matchCode, e);
        } finally {
            if (acquired) {
                lock.unlock();
            }
        }
    }
}
