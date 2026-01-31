package com.supabase.qnasession.service;

import com.supabase.qnasession.models.Cluster;
import com.supabase.qnasession.models.Message;
import com.supabase.qnasession.models.Room;
import com.supabase.qnasession.repository.ClusterRepository;
import com.supabase.qnasession.repository.MessageRepository;
import com.supabase.qnasession.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageClusteringScheduler {

    private final MessageRepository messageRepository;
    private final ClusterRepository clusterRepository;
    private final RoomRepository roomRepository;
    private final TopicClustererService topicClustererService;

    @Scheduled(
        fixedDelayString = "${clustering.poll-interval-ms:15000}",
        initialDelayString = "${clustering.initial-delay-ms:5000}"
    )
    @Transactional
    public void pollAndCluster() {
        List<String> roomIds = getRoomIds();
        if (roomIds.isEmpty()) {
            log.debug("No rooms found for clustering");
            return;
        }

        for (String roomId : roomIds) {
            if (roomId == null || roomId.isBlank()) {
                continue;
            }

            List<Message> pending = messageRepository
                .findByRoomIdAndClusterIdIsNullOrderByCreatedAtAsc(roomId);

            if (pending.isEmpty()) {
                continue;
            }

            Map<UUID, String> topicsByMessageId = topicClustererService.clusterTopics(roomId, pending);
            if (topicsByMessageId.isEmpty()) {
                log.warn("No topics returned for room {}; skipping clustering", roomId);
                continue;
            }

            Map<String, List<Message>> byTopic = new HashMap<>();
            Map<String, String> displayTopicByKey = new HashMap<>();
            for (Message message : pending) {
                String topic = topicsByMessageId.get(message.getId());
                if (topic == null || topic.isBlank()) {
                    continue;
                }
                String normalized = normalizeTopic(topic);
                if (normalized.isBlank()) {
                    continue;
                }
                displayTopicByKey.putIfAbsent(normalized, topic.trim());
                byTopic.computeIfAbsent(normalized, key -> new ArrayList<>()).add(message);
            }

            if (byTopic.isEmpty()) {
                log.warn("No valid topic assignments for room {}; skipping clustering", roomId);
                continue;
            }

            for (Map.Entry<String, List<Message>> entry : byTopic.entrySet()) {
                String topicKey = entry.getKey();
                String topic = displayTopicByKey.getOrDefault(topicKey, topicKey);
                List<Message> bucket = entry.getValue();

                Cluster cluster = clusterRepository
                    .findFirstByRoomIdAndTitle(roomId, topic)
                    .orElseGet(() -> {
                        Cluster created = new Cluster();
                        created.setRoomId(roomId);
                        created.setTitle(topic);
                        created.setMessageCount(0);
                        return created;
                    });

                int currentCount = cluster.getMessageCount() == null ? 0 : cluster.getMessageCount();
                cluster.setKeywords(topic);
                cluster.setMessageCount(currentCount + bucket.size());
                cluster = clusterRepository.save(cluster);

                UUID clusterId = cluster.getId();
                for (Message message : bucket) {
                    message.setClusterId(clusterId);
                }
                messageRepository.saveAll(bucket);

                log.info("Clustered {} messages for room {} into topic '{}' (cluster {})",
                    bucket.size(), roomId, topic, clusterId);
            }
        }
    }

    private List<String> getRoomIds() {
        List<Room> rooms = roomRepository.findAll();
        if (!rooms.isEmpty()) {
            List<String> ids = new ArrayList<>(rooms.size());
            for (Room room : rooms) {
                if (room != null && room.getId() != null) {
                    ids.add(room.getId());
                }
            }
            return ids;
        }
        return messageRepository.findDistinctRoomIds();
    }

    private String normalizeTopic(String topic) {
        String trimmed = topic.trim();
        int end = trimmed.length();
        while (end > 0) {
            char ch = trimmed.charAt(end - 1);
            if (Character.isLetterOrDigit(ch) || ch == ' ') {
                break;
            }
            end--;
        }
        String cleaned = trimmed.substring(0, end).replaceAll("\\s+", " ").trim();
        return cleaned.toLowerCase();
    }
}
