package com.supabase.qnasession.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supabase.qnasession.config.GeminiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiClient {

    private final GeminiConfig geminiConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    public Optional<String> generateText(String prompt) {
        if (!geminiConfig.isEnabled()) {
            return Optional.empty();
        }
        if (geminiConfig.getApiKey() == null || geminiConfig.getApiKey().isBlank()) {
            log.warn("Gemini API key is missing; skipping clustering");
            return Optional.empty();
        }

        try {
            String url = geminiConfig.getBaseUrl() + "/models/" + geminiConfig.getModel() + ":generateContent";
            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of("role", "user", "parts", List.of(part));
            Map<String, Object> body = new HashMap<>();
            body.put("contents", List.of(content));
            body.put("generationConfig", Map.of("temperature", 0.2));

            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("x-goog-api-key", geminiConfig.getApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                log.warn("Gemini API error {}: {}", response.statusCode(), response.body());
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return Optional.empty();
            }
            JsonNode text = candidates.get(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode()) {
                return Optional.empty();
            }
            return Optional.ofNullable(text.asText());
        } catch (Exception e) {
            log.error("Failed to call Gemini API", e);
            return Optional.empty();
        }
    }
}
