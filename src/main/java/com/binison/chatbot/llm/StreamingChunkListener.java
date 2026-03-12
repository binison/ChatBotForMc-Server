package com.binison.chatbot.llm;

@FunctionalInterface
public interface StreamingChunkListener {
    void onDelta(String text);
}
