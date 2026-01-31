package com.supabase.qnasession.realtime;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
public class RealtimeEvent {
    
    private String event;
    private String topic;
    private String ref;
    private RealtimePayload payload;
    
    @Data
    public static class RealtimePayload {
        private String status;
        private RealtimeRecord data;
        private String type; // INSERT, UPDATE, DELETE
        private Map<String, Object> columns;
        private Map<String, Object> commit_timestamp;
        private Map<String, Object> errors;
        
        @Data
        public static class RealtimeRecord {
            private String table;
            private String type;
            private Map<String, Object> record;
            private Map<String, Object> old_record;
            private String schema;
            
            @JsonProperty("commit_timestamp")
            private String commitTimestamp;
        }
    }
}
