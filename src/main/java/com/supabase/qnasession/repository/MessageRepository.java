package com.supabase.qnasession.repository;

import com.supabase.qnasession.models.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByRoomIdAndClusterIdIsNullOrderByCreatedAtAsc(String roomId);

    @Query("select distinct m.roomId from Message m where m.roomId is not null")
    List<String> findDistinctRoomIds();
}
