package com.backend.Controller;

import com.backend.Entity.Room;
import com.backend.Exceptions.RoomExceptions;
import com.backend.Repository.PlayerRepository;
import com.backend.Repository.RoomRepository;
import com.backend.Service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LobbyWebSocketController {

    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;
//    private final RoomService roomService;

    @MessageMapping("/player.register")
    public void registerPlayerSession(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String playerId = payload.get("playerId");
        String roomCode = payload.get("roomCode");
        String sessionId = headerAccessor.getSessionId();

        log.debug("Registering session {} for player {} in room {}", sessionId, playerId, roomCode);

        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RoomExceptions.RoomNotFoundException(roomCode));

        playerRepository.findByPlayerIdAndRoom(playerId, room)
                .ifPresent(player -> {
                    player.setSocketSessionId(sessionId);
                    playerRepository.save(player);
                    log.debug("Session {} registered to player '{}'", sessionId, player.getPlayerName());
                });
    }
}
