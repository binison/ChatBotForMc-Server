package com.binison.chatbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionManagerTest {
    @Test
    void contextIncludesSystemPromptAndHistory() {
        SessionManager manager = new SessionManager(30);
        UUID playerId = UUID.randomUUID();
        manager.appendUser(playerId, "hello");
        manager.appendAssistant(playerId, "hi");
        assertEquals(3, manager.getContext(playerId, true, 4, "system").size());
    }

    @Test
    void contextUsesProvidedSystemPrompt() {
        SessionManager manager = new SessionManager(30);
        UUID playerId = UUID.randomUUID();

        List<com.binison.chatbot.model.ChatMessage> context = manager.getContext(playerId, true, 4, "global system");
        assertEquals("global system", context.get(0).content());
    }

    @Test
    void updateExpireAfterKeepsContextWorking() {
        SessionManager manager = new SessionManager(30);
        UUID playerId = UUID.randomUUID();
        manager.appendUser(playerId, "hello");

        manager.updateExpireAfter(1);

        assertEquals(2, manager.getContext(playerId, true, 4, "system").size());
    }

    @Test
    void contextOnlyReturnsMostRecentRounds() {
        SessionManager manager = new SessionManager(30);
        UUID playerId = UUID.randomUUID();
        manager.appendUser(playerId, "u1");
        manager.appendAssistant(playerId, "a1");
        manager.appendUser(playerId, "u2");
        manager.appendAssistant(playerId, "a2");
        manager.appendUser(playerId, "u3");
        manager.appendAssistant(playerId, "a3");

        List<com.binison.chatbot.model.ChatMessage> context = manager.getContext(playerId, true, 2, "system");
        assertEquals(5, context.size());
        assertEquals("u2", context.get(1).content());
        assertEquals("a3", context.get(4).content());
    }
}
