package com.supabase.qnasession.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supabase.qnasession.config.SupabaseConfig;
import com.supabase.qnasession.realtime.RealtimeChangeListener;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class SupabaseRealtimeService {
    
    private final SupabaseConfig supabaseConfig;
    private final ObjectMapper objectMapper;
    private WebSocketClient client;
    private final AtomicInteger refCounter = new AtomicInteger(0);
    private final List<RealtimeChangeListener> listeners = new CopyOnWriteArrayList<>();
    
    @PostConstruct
    public void connect() {
        if (!supabaseConfig.getRealtime().isEnabled()) {
            log.info("Supabase Realtime is disabled");
            return;
        }

        if (supabaseConfig.getAnonKey() == null || supabaseConfig.getAnonKey().isBlank()) {
            log.error("Supabase anon key is missing; realtime connection will not be attempted");
            return;
        }
        
        try {
            String wsUrl = supabaseConfig.getRealtimeWsUrl() + 
                          "?apikey=" + supabaseConfig.getAnonKey() + 
                          "&vsn=1.0.0";
            
            log.info("Connecting to Supabase Realtime: {}", wsUrl.replaceAll("apikey=[^&]*", "apikey=***"));
            
            Map<String, String> headers = new HashMap<>();
            headers.put("apikey", supabaseConfig.getAnonKey());
            headers.put("Authorization", "Bearer " + supabaseConfig.getAnonKey());

            client = new WebSocketClient(new URI(wsUrl), new Draft_6455(), headers, 10000) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    log.info("WebSocket connection opened");
                    subscribeToTables();
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    log.warn("WebSocket connection closed: {} - {}", code, reason);
                    // Attempt to reconnect after 5 seconds in a separate thread
                    scheduleReconnect();
                }

                @Override
                public void onError(Exception ex) {
                    log.error("WebSocket error", ex);
                }
            };
            
            // Add Authorization header
            client.addHeader("Authorization", "Bearer " + supabaseConfig.getAnonKey());
            client.addHeader("apikey", supabaseConfig.getAnonKey());
            
            client.connect();
            
        } catch (Exception e) {
            log.error("Failed to connect to Supabase Realtime", e);
        }
    }
    
    private void subscribeToTables() {
        List<String> tables = supabaseConfig.getRealtime().getTables();
        
        if (tables.isEmpty()) {
            log.warn("No tables configured for realtime subscription");
            return;
        }
        
        for (String table : tables) {
            subscribeToTable(table);
        }
    }
    
    private void subscribeToTable(String table) {
        try {
            String ref = String.valueOf(refCounter.incrementAndGet());
            
            Map<String, Object> message = new HashMap<>();
            message.put("topic", "realtime:public:" + table);
            message.put("event", "phx_join");
            message.put("payload", Map.of());
            message.put("ref", ref);
            
            String jsonMessage = objectMapper.writeValueAsString(message);
            client.send(jsonMessage);
            
            log.info("Subscribed to table: {}", table);
            
        } catch (Exception e) {
            log.error("Failed to subscribe to table: {}", table, e);
        }
    }
    
    private void handleMessage(String message) {
        try {
            JsonNode root = objectMapper.readTree(message);
            String event = root.path("event").asText();
            
            // Handle heartbeat
            if ("phx_reply".equals(event)) {
                log.debug("Received reply: {}", message);
                return;
            }
            
            if ("postgres_changes".equals(event)) {
                handlePostgresChange(root);
            }
            
        } catch (Exception e) {
            log.error("Failed to parse realtime message: {}", message, e);
        }
    }
    
    private void handlePostgresChange(JsonNode root) {
        try {
            JsonNode payload = root.path("payload");
            JsonNode data = payload.path("data");
            
            String type = data.path("type").asText(); // INSERT, UPDATE, DELETE
            String table = data.path("table").asText();
            
            log.info("Received {} event for table: {}", type, table);
            
            switch (type.toUpperCase()) {
                case "INSERT":
                    Map<String, Object> newRecord = objectMapper.convertValue(
                        data.path("record"), Map.class);
                    notifyInsert(table, newRecord);
                    break;
                    
                case "UPDATE":
                    Map<String, Object> updatedRecord = objectMapper.convertValue(
                        data.path("record"), Map.class);
                    Map<String, Object> oldRecord = objectMapper.convertValue(
                        data.path("old_record"), Map.class);
                    notifyUpdate(table, updatedRecord, oldRecord);
                    break;
                    
                case "DELETE":
                    Map<String, Object> deletedRecord = objectMapper.convertValue(
                        data.path("old_record"), Map.class);
                    notifyDelete(table, deletedRecord);
                    break;
                    
                default:
                    log.warn("Unknown change type: {}", type);
            }
            
        } catch (Exception e) {
            log.error("Failed to handle postgres change", e);
        }
    }
    
    public void addListener(RealtimeChangeListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(RealtimeChangeListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyInsert(String table, Map<String, Object> record) {
        for (RealtimeChangeListener listener : listeners) {
            try {
                listener.onInsert(table, record);
            } catch (Exception e) {
                log.error("Error notifying listener of insert", e);
            }
        }
    }
    
    private void notifyUpdate(String table, Map<String, Object> newRecord, Map<String, Object> oldRecord) {
        for (RealtimeChangeListener listener : listeners) {
            try {
                listener.onUpdate(table, newRecord, oldRecord);
            } catch (Exception e) {
                log.error("Error notifying listener of update", e);
            }
        }
    }
    
    private void notifyDelete(String table, Map<String, Object> oldRecord) {
        for (RealtimeChangeListener listener : listeners) {
            try {
                listener.onDelete(table, oldRecord);
            } catch (Exception e) {
                log.error("Error notifying listener of delete", e);
            }
        }
    }
    
    private void scheduleReconnect() {
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                log.info("Attempting to reconnect...");
                if (client != null) {
                    client.close();
                }
                connect();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Reconnection interrupted", e);
            }
        }, "supabase-reconnect").start();
    }
    
    @PreDestroy
    public void disconnect() {
        if (client != null && client.isOpen()) {
            client.close();
            log.info("WebSocket connection closed");
        }
    }
}
