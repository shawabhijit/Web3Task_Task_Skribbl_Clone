package com.backend.Repository;

import com.backend.Entity.Player;
import com.backend.Entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlayerRepository extends JpaRepository<Player , Long> {

    Optional<Player> findByPlayerIdAndRoom(String playerId, Room room);

    Optional<Player> findByPlayerIdAndRoom_RoomCode(String playerId, String roomCode);

    List<Player> findByRoom(Room room);

    boolean existsByPlayerIdAndRoom(String playerId, Room room);

    /**
     * Find by socket session — used on WebSocket disconnect to identify who left.
     */
    Optional<Player> findBySocketSessionId(String socketSessionId);
}
