package com.binison.chatbot.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class PluginConfigEventTriggersTest {
    @Test
    void advancementTriggerIgnoresConfiguredPrefixes() {
        PluginConfig.AdvancementTrigger trigger = new PluginConfig.AdvancementTrigger(
                true,
                false,
                180,
                "prompt",
                List.of("minecraft:recipes/", "custom:spam/")
        );

        assertTrue(trigger.shouldIgnore("minecraft:recipes/redstone/clock"));
        assertTrue(trigger.shouldIgnore("custom:spam/test"));
        assertFalse(trigger.shouldIgnore("minecraft:story/mine_diamond"));
    }

    @Test
    void eventTriggersStoreBroadcastMode() {
        PluginConfig.EventTriggers triggers = new PluginConfig.EventTriggers(
                true,
                false,
                new PluginConfig.EventTrigger(true, true, 300, "join"),
                new PluginConfig.EventTrigger(true, false, 180, "death"),
                new PluginConfig.AdvancementTrigger(true, false, 180, "advancement", List.of("minecraft:recipes/"))
        );

        assertFalse(triggers.broadcast());
        assertTrue(triggers.join().enabled());
    }
}

