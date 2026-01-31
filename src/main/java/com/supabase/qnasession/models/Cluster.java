package com.supabase.qnasession.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
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
    
    @Column(name = "keywords", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> keywords;
    
    @Column(name = "message_count")
    private Integer messageCount;
    
    @Column(name = "updated_at")
    private Instant updatedAt;
    
    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}

