package com.backend.Utils;

import com.backend.Entity.Player;
import com.backend.Repository.PlayerRepository;
import com.backend.Service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final PlayerRepository playerRepository;
    private final RoomService roomService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        log.debug("WebSocket client connected: sessionId={}", sessionId);
        // Note: At connection time we don't yet know which player this is.
        // The player updates their socketSessionId via the STOMP subscribe message
        // (handled in LobbyWebSocketController.handleSubscribe).
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("WebSocket client disconnected: sessionId={}", sessionId);

        // Look up which player had this session
        Optional<Player> playerOpt = playerRepository.findBySocketSessionId(sessionId);

        if (playerOpt.isEmpty()) {
            log.debug("No player found for disconnected session {}", sessionId);
            return;
        }

        Player player = playerOpt.get();
        String roomCode = player.getRoom().getRoomCode();

        log.info("Player '{}' (id: {}) disconnected from room {}",
                player.getPlayerName(), player.getPlayerId(), roomCode);

        // Trigger leave logic: handles host promotion, broadcast, etc.
        roomService.leaveRoom(player.getPlayerId(), roomCode);
    }
}
