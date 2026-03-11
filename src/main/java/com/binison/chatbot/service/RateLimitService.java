package com.binison.chatbot.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitService {
    private final Map<UUID, Instant> lastRequestTime = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> inFlight = new ConcurrentHashMap<>();

    public boolean isCoolingDown(UUID playerId, long cooldownSeconds) {
        Instant last = lastRequestTime.get(playerId);
        return last != null && last.plusSeconds(cooldownSeconds).isAfter(Instant.now());
    }

    public boolean tryAcquire(UUID playerId) {
        return inFlight.putIfAbsent(playerId, Boolean.TRUE) == null;
    }

    public void markCompleted(UUID playerId) {
        inFlight.remove(playerId);
        lastRequestTime.put(playerId, Instant.now());
    }

    public void clear(UUID playerId) {
        inFlight.remove(playerId);
        lastRequestTime.remove(playerId);
    }
}
