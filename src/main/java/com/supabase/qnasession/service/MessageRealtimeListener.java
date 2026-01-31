package com.supabase.qnasession.service;

import com.supabase.qnasession.realtime.RealtimeChangeListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Example listener that handles realtime changes to the messages table
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MessageRealtimeListener implements RealtimeChangeListener {
    
    private final SupabaseRealtimeService realtimeService;
    
    @PostConstruct
    public void init() {
        // Register this listener with the realtime service
        realtimeService.addListener(this);
        log.info("MessageRealtimeListener registered");
    }
    
    @Override
    public void onInsert(String table, Map<String, Object> record) {
        if ("messages".equals(table)) {
            log.info("New message inserted: {}", record);
            
            // Extract message data
            String messageId = String.valueOf(record.get("id"));
            String content = String.valueOf(record.get("content"));
            String userName = String.valueOf(record.get("user_name"));
            String roomId = String.valueOf(record.get("room_id"));
            
            // TODO: Add your business logic here
            // For example:
            // - Send notification to connected WebSocket clients
            // - Update cache
            // - Trigger clustering algorithm
            // - Send email notifications
            
            log.info("Message from {} in room {}: {}", userName, roomId, content);
        }
    }
    
    @Override
    public void onUpdate(String table, Map<String, Object> newRecord, Map<String, Object> oldRecord) {
        if ("messages".equals(table)) {
            log.info("Message updated: old={}, new={}", oldRecord, newRecord);
            
            // TODO: Handle message updates
            // For example:
            // - Update vote count
            // - Re-cluster if content changed
            // - Notify relevant users
        }
    }
    
    @Override
    public void onDelete(String table, Map<String, Object> oldRecord) {
        if ("messages".equals(table)) {
            log.info("Message deleted: {}", oldRecord);
            
            // TODO: Handle message deletion
            // For example:
            // - Remove from cache
            // - Update cluster
            // - Notify users
        }
    }
}
