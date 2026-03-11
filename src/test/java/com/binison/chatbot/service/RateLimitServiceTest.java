package com.binison.chatbot.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RateLimitServiceTest {
    @Test
    void acquireAndCooldownWorkTogether() {
        RateLimitService service = new RateLimitService();
        UUID playerId = UUID.randomUUID();
        assertTrue(service.tryAcquire(playerId));
        assertFalse(service.tryAcquire(playerId));
        service.markCompleted(playerId);
        assertTrue(service.isCoolingDown(playerId, 3));
    }
}

