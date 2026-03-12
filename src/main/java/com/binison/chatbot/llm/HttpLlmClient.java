package com.binison.chatbot.llm;

import com.binison.chatbot.config.PluginConfig;
import com.binison.chatbot.model.ChatMessage;
import com.binison.chatbot.model.LlmResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
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
        String requestBody = objectMapper.writeValueAsString(buildPayload(messages, false));
        HttpResponse<String> response = httpClient.send(buildRequest(requestBody), HttpResponse.BodyHandlers.ofString());
        ensureSuccess(response.statusCode(), response.body());

        JsonNode root = objectMapper.readTree(response.body());
        String content = extractChatContent(root, response.body());
        return new LlmResponse(content);
    }

    @Override
    public void streamChat(List<ChatMessage> messages, StreamingChunkListener listener) throws IOException, InterruptedException {
        if (!config.streamEnabled()) {
            LlmClient.super.streamChat(messages, listener);
            return;
        }

        String requestBody = objectMapper.writeValueAsString(buildPayload(messages, true));
        HttpResponse<InputStream> response = httpClient.send(buildRequest(requestBody), HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            try (InputStream errorStream = response.body()) {
                String errorBody = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                ensureSuccess(response.statusCode(), errorBody);
            }
            return;
        }

        try (InputStream stream = response.body();
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith(":")) {
                    continue;
                }
                if (!line.startsWith("data:")) {
                    continue;
                }

                String data = line.substring(5).trim();
                if (data.isEmpty() || "[DONE]".equals(data)) {
                    if ("[DONE]".equals(data)) {
                        return;
                    }
                    continue;
                }

                JsonNode root = objectMapper.readTree(data);
                JsonNode deltaNode = root.path("choices").path(0).path("delta");
                JsonNode contentNode = deltaNode.path("content");
                if (contentNode.isTextual()) {
                    String delta = contentNode.asText();
                    if (!delta.isEmpty()) {
                        listener.onDelta(delta);
                    }
                } else if (contentNode.isArray()) {
                    for (JsonNode part : contentNode) {
                        JsonNode textNode = part.path("text");
                        if (textNode.isTextual() && !textNode.asText().isEmpty()) {
                            listener.onDelta(textNode.asText());
                        }
                    }
                }
            }
        }
    }

    private Map<String, Object> buildPayload(List<ChatMessage> messages, boolean stream) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("model", config.model());
        payload.put("messages", messages);
        payload.put("temperature", config.temperature());
        if (config.maxTokens() > 0) {
            payload.put("max_tokens", config.maxTokens());
        }
        if (stream) {
            payload.put("stream", true);
        }
        return payload;
    }

    private HttpRequest buildRequest(String requestBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(config.apiBaseUrl()))
                .timeout(Duration.ofMillis(config.timeoutMs()))
                .header("Content-Type", "application/json")
                .header(config.authHeader(), config.authorizationValue())
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    private void ensureSuccess(int statusCode, String body) throws IOException {
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException("LLM request failed with status " + statusCode + ", body: " + previewBody(body));
        }
    }

    private String extractChatContent(JsonNode root, String rawBody) throws IOException {
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new IOException("LLM response did not contain content. body: " + previewBody(rawBody));
        }
        return contentNode.asText();
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
