package com.binison.chatbot.llm;

import com.binison.chatbot.model.ChatMessage;
import com.binison.chatbot.model.LlmResponse;
import java.io.IOException;
import java.util.List;

public interface LlmClient {
    LlmResponse chat(List<ChatMessage> messages) throws IOException, InterruptedException;

    default void streamChat(List<ChatMessage> messages, StreamingChunkListener listener) throws IOException, InterruptedException {
        LlmResponse response = chat(messages);
        if (response.content() != null && !response.content().isBlank()) {
            listener.onDelta(response.content());
        }
    }
}
