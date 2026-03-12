package com.binison.chatbot.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.binison.chatbot.model.ChatMessage;
import com.binison.chatbot.model.LlmResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class LlmClientStreamingFallbackTest {
    @Test
    void defaultStreamChatFallsBackToSingleDelta() throws IOException, InterruptedException {
        LlmClient client = messages -> new LlmResponse("stream fallback");
        List<String> deltas = new ArrayList<>();

        client.streamChat(List.of(new ChatMessage("user", "hi")), deltas::add);

        assertEquals(List.of("stream fallback"), deltas);
    }
}
