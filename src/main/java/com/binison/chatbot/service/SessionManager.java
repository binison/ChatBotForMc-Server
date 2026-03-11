package com.binison.chatbot.service;

import com.binison.chatbot.model.ChatMessage;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final Map<UUID, SessionState> sessions = new ConcurrentHashMap<>();
    private Duration expireAfter;

    public SessionManager(long expireMinutes) {
        this.expireAfter = Duration.ofMinutes(Math.max(1, expireMinutes));
    }

    public void updateExpireAfter(long expireMinutes) {
        this.expireAfter = Duration.ofMinutes(Math.max(1, expireMinutes));
    }

    public List<ChatMessage> getContext(UUID playerId, boolean enabled, int maxRounds, String systemPrompt) {
        purgeExpired();
        List<ChatMessage> result = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            result.add(new ChatMessage("system", systemPrompt));
        }
        if (!enabled) {
            return result;
        }
        SessionState state = sessions.get(playerId);
        if (state == null) {
            return result;
        }
        result.addAll(state.snapshot(maxRounds));
        return result;
    }

    public void appendUser(UUID playerId, String content) {
        sessions.computeIfAbsent(playerId, ignored -> new SessionState()).append(new ChatMessage("user", content));
    }

    public void appendAssistant(UUID playerId, String content) {
        sessions.computeIfAbsent(playerId, ignored -> new SessionState()).append(new ChatMessage("assistant", content));
    }

    public void reset(UUID playerId) {
        sessions.remove(playerId);
    }

    private void purgeExpired() {
        Instant now = Instant.now();
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(now, expireAfter));
    }

    private static final class SessionState {
        private final List<ChatMessage> messages = new ArrayList<>();
        private Instant lastTouched = Instant.now();

        private synchronized List<ChatMessage> snapshot(int maxRounds) {
            int from = Math.max(0, messages.size() - maxRounds * 2);
            List<ChatMessage> snapshot = new ArrayList<>(messages.subList(from, messages.size()));
            lastTouched = Instant.now();
            return snapshot;
        }

        private synchronized void append(ChatMessage message) {
            messages.add(message);
            lastTouched = Instant.now();
        }

        private synchronized boolean isExpired(Instant now, Duration expireAfter) {
            return Duration.between(lastTouched, now).compareTo(expireAfter) > 0;
        }
    }
}
