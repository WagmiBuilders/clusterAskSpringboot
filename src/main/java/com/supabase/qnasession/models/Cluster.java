package com.supabase.qnasession.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clusters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cluster {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @Column(name = "room_id")
    private String roomId;
    
    @Column(name = "title", columnDefinition = "text")
    private String title;
    
    @Column(name = "keywords", columnDefinition = "text")
    private String keywords;
    
    @Column(name = "message_count")
    private Integer messageCount;

    @Column(name = "votes")
    private Integer votes;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        if (messageCount == null) {
            messageCount = 0;
        }
        if (votes == null) {
            votes = 0;
        }
        updatedAt = Instant.now();
    }
}

