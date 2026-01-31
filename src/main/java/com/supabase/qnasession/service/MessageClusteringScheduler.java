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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageClusteringScheduler {

    private static final String CLUSTER_TITLE_PREFIX = "Auto cluster for ";

    private final MessageRepository messageRepository;
    private final ClusterRepository clusterRepository;
    private final RoomRepository roomRepository;

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

            String title = CLUSTER_TITLE_PREFIX + roomId;
            Cluster cluster = clusterRepository
                .findFirstByRoomIdAndTitle(roomId, title)
                .orElseGet(() -> {
                    Cluster created = new Cluster();
                    created.setRoomId(roomId);
                    created.setTitle(title);
                    created.setMessageCount(0);
                    return created;
                });

            int currentCount = cluster.getMessageCount() == null ? 0 : cluster.getMessageCount();
            cluster.setKeywords(extractKeywords(pending));
            cluster.setMessageCount(currentCount + pending.size());
            cluster = clusterRepository.save(cluster);

            UUID clusterId = cluster.getId();
            for (Message message : pending) {
                message.setClusterId(clusterId);
            }
            messageRepository.saveAll(pending);

            log.info("Clustered {} messages for room {} into cluster {}", pending.size(), roomId, clusterId);
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

    private String extractKeywords(List<Message> messages) {
        Map<String, Integer> counts = new HashMap<>();
        for (Message message : messages) {
            if (message == null || message.getContent() == null) {
                continue;
            }
            String[] tokens = message.getContent()
                .toLowerCase(Locale.ROOT)
                .split("[^a-z0-9]+");
            for (String token : tokens) {
                if (token.length() < 3) {
                    continue;
                }
                counts.merge(token, 1, Integer::sum);
            }
        }

        if (counts.isEmpty()) {
            return null;
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort(
            Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                .reversed()
                .thenComparing(Map.Entry::getKey)
        );

        int limit = Math.min(5, entries.size());
        List<String> top = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            top.add(entries.get(i).getKey());
        }
        return String.join(",", top);
    }
}
