package com.supabase.qnasession.repository;

import com.supabase.qnasession.models.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ClusterRepository extends JpaRepository<Cluster, UUID> {

    Optional<Cluster> findFirstByRoomIdAndTitle(String roomId, String title);
}
