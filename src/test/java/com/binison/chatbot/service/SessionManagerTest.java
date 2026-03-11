package com.binison.chatbot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}

