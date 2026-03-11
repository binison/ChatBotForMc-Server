package com.binison.chatbot.llm;

import com.binison.chatbot.model.ChatMessage;
import com.binison.chatbot.model.LlmResponse;
import java.io.IOException;
import java.util.List;

public interface LlmClient {
    LlmResponse chat(List<ChatMessage> messages) throws IOException, InterruptedException;
}
