package com.supabase.qnasession.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "supabase")
@Getter
@Setter
public class SupabaseConfig {
    
    private String projectUrl;
    private String anonKey;
    private Realtime realtime = new Realtime();
    
    @Getter
    @Setter
    public static class Realtime {
        private boolean enabled = true;
        private List<String> tables = List.of();
    }
    
    public String getRealtimeWsUrl() {
        // Convert https://project.supabase.co to wss://project.supabase.co/realtime/v1/websocket
        return projectUrl.replace("https://", "wss://") + "/realtime/v1/websocket";
    }
}
