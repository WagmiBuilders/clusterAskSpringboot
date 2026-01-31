package com.supabase.qnasession.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "cluster_votes",
    uniqueConstraints = @jakarta.persistence.UniqueConstraint(
        name = "cluster_votes_cluster_id_user_id_key",
        columnNames = {"cluster_id", "user_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ClusterVote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "cluster_id")
    private UUID clusterId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
