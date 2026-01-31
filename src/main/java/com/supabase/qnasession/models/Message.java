package com.supabase.qnasession.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "room_id")
    private String roomId;
    
    @Column(name = "event_id")
    private String eventId;
    
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "content", columnDefinition = "text")
    private String content;
    
    @Column(name = "created_at")
    private Instant createdAt;
    
    @Column(name = "cluster_id")
    private UUID clusterId;
    
    @Column(name = "clustered_at")
    private Instant clusteredAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

