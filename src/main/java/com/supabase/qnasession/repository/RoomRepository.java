package com.supabase.qnasession.repository;

import com.supabase.qnasession.models.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, String> {
}
