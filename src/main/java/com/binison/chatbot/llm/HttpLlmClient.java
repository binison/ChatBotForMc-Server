package com.binison.chatbot.llm;

import com.binison.chatbot.config.PluginConfig;
import com.binison.chatbot.model.ChatMessage;
import com.binison.chatbot.model.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpLlmClient implements LlmClient {
    private static final int ERROR_BODY_PREVIEW_LENGTH = 300;

    private final PluginConfig config;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public HttpLlmClient(PluginConfig config) {
        this(config, HttpClient.newBuilder().connectTimeout(Duration.ofMillis(config.timeoutMs())).build(), new ObjectMapper());
    }

    public HttpLlmClient(PluginConfig config, HttpClient httpClient, ObjectMapper objectMapper) {
        this.config = config;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmResponse chat(List<ChatMessage> messages) throws IOException, InterruptedException {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", config.model());
        payload.put("messages", messages);
        payload.put("temperature", config.temperature());
        payload.put("max_tokens", config.maxTokens());

        String requestBody = objectMapper.writeValueAsString(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.apiBaseUrl()))
                .timeout(Duration.ofMillis(config.timeoutMs()))
                .header("Content-Type", "application/json")
                .header(config.authHeader(), config.authorizationValue())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("LLM request failed with status " + response.statusCode() + ", body: " + previewBody(response.body()));
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new IOException("LLM response did not contain content. body: " + previewBody(response.body()));
        }
        return new LlmResponse(contentNode.asText());
    }

    private String previewBody(String body) {
        if (body == null || body.isBlank()) {
            return "<empty>";
        }
        String normalized = body.replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.length() <= ERROR_BODY_PREVIEW_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, ERROR_BODY_PREVIEW_LENGTH) + "...";
    }
}
