package com.backend.Repository;

import com.backend.Entity.Enum.RoomStatus;
import com.backend.Entity.Enum.RoomType;
import com.backend.Entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room , Long> {
    Optional<Room> findByRoomCode(String roomCode);

    boolean existsByRoomCode(String roomCode);

    /**
     * Find all public rooms that are currently in WAITING state and not full.
     * Used for the "Join Public Room" lobby browser.
     *
     * The subquery counts only CONNECTED players to exclude disconnected ghosts.
     */
    @Query("""
        SELECT r FROM Room r
        WHERE r.roomType = :roomType
          AND r.status = :status
          AND (
              SELECT COUNT(p) FROM Player p
              WHERE p.room = r
                AND p.status = com.backend.Entity.Enum.PlayerStatus.CONNECTED
          ) < r.settings.maxPlayers
        ORDER BY r.createdAt DESC
    """)
    List<Room> findJoinablePublicRooms(
            @Param("roomType") RoomType roomType,
            @Param("status") RoomStatus status
    );

    /**
     * Find room with players eagerly loaded — avoids N+1 when we need
     * to broadcast the full player list right after join.
     */
    @Query("""
        SELECT r FROM Room r
        LEFT JOIN FETCH r.players p
        WHERE r.roomCode = :roomCode
    """)
    Optional<Room> findByRoomCodeWithPlayers(@Param("roomCode") String roomCode);
}
