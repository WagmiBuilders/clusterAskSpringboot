package com.supabase.qnasession.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supabase.qnasession.models.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicClustererService {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public Map<UUID, String> clusterTopics(String roomId, List<Message> messages) {
        if (messages.isEmpty()) {
            return Map.of();
        }

        String prompt = buildPrompt(roomId, messages);
        Optional<String> response = geminiClient.generateText(prompt);
        if (response.isEmpty()) {
            return Map.of();
        }

        String json = extractJson(response.get());
        if (json == null) {
            log.warn("Gemini response did not contain JSON for room {}", roomId);
            return Map.of();
        }

        return parseAssignments(json);
    }

    private String buildPrompt(String roomId, List<Message> messages) {
        StringBuilder builder = new StringBuilder();
        builder.append("You are clustering Q&A messages into topics.\n");
        builder.append("Return ONLY JSON array (no markdown) with objects: ");
        builder.append("{\"message_id\":\"<uuid>\",\"topic\":\"<short noun phrase>\"}.\n");
        builder.append("Rules:\n");
        builder.append("- Topic should be 1-3 words.\n");
        builder.append("- Keep proper nouns (e.g., RagJN).\n");
        builder.append("- Remove filler words like explain, about, what is, how to.\n");
        builder.append("- If unclear, use \"general\".\n");
        builder.append("Room: ").append(roomId).append("\n");
        builder.append("Messages:\n");
        for (Message message : messages) {
            builder.append("- id: ").append(message.getId())
                .append(" content: ").append(message.getContent()).append("\n");
        }
        return builder.toString();
    }

    private String extractJson(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("[")) {
            return trimmed;
        }
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return null;
    }

    private Map<UUID, String> parseAssignments(String json) {
        Map<UUID, String> results = new HashMap<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isArray()) {
                return results;
            }
            for (JsonNode node : root) {
                String idText = node.path("message_id").asText(null);
                String topic = node.path("topic").asText(null);
                if (idText == null || topic == null || topic.isBlank()) {
                    continue;
                }
                try {
                    UUID id = UUID.fromString(idText);
                    results.put(id, topic.trim());
                } catch (IllegalArgumentException ignored) {
                    // ignore invalid UUIDs
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse Gemini topic assignments", e);
        }
        return results;
    }
}
