package com.backend.Service;

import com.backend.DTO.PlayerDto;
import com.backend.Entity.Enum.PlayerStatus;
import com.backend.Entity.Enum.RoomStatus;
import com.backend.Entity.Enum.RoomType;
import com.backend.Entity.Player;
import com.backend.Exceptions.RoomExceptions;
import com.backend.Repository.PlayerRepository;
import com.backend.Utils.LobbyEvent;
import com.backend.Utils.RoomCodeGenerator;
import com.backend.exchanges.CreateRoomRequest;
import com.backend.exchanges.JoinRoomRequest;
import com.backend.exchanges.PublicRoomSummaryResponse;
import com.backend.exchanges.RoomResponse;
import com.backend.Entity.Room;
import com.backend.Entity.RoomSettings;
import com.backend.Repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final PlayerRepository playerRepository;
    private final RoomCodeGenerator codeGenerator;
    private final SimpMessagingTemplate messagingTemplate;

    /** @Transactional ensures the room + player are saved atomically.
      * If player creation fails, the room is rolled back too.
     */
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest request) {
        log.info("Creating room for player '{}' (id: {}), type: {}",
                request.getPlayerName(), request.getPlayerId(), request.getRoomType());

        String roomCode = codeGenerator.generateUniqueCode();

        RoomSettings settings = RoomSettings.builder()
                .maxPlayers(request.getMaxPlayers())
                .rounds(request.getRounds())
                .drawTimeSeconds(request.getDrawTimeSeconds())
                .wordCount(request.getWordCount())
                .hints(request.getHints())
                .wordMode(request.getWordMode())
                .build();

        Room room = Room.builder()
                .roomCode(roomCode)
                .roomType(request.getRoomType())
                .hostPlayerId(request.getPlayerId())
                .settings(settings)
                .status(RoomStatus.WAITING)
                .build();

        room = roomRepository.save(room);

        // Add the creator as the first (host) player
        Player host = Player.builder()
                .playerId(request.getPlayerId())
                .playerName(request.getPlayerName())
                .room(room)
                .isHost(true)
                .status(PlayerStatus.CONNECTED)
                .score(0)
                .build();

        playerRepository.save(host);
        room.getPlayers().add(host);

        log.info("Room created: {} (code: {})", room.getId(), roomCode);
        return RoomResponse.from(room);
    }


    @Transactional
    public RoomResponse joinRoom(JoinRoomRequest request) {
        log.info("Player '{}' (id: {}) attempting to join room: {}",
                request.getPlayerName(), request.getPlayerId(), request.getRoomCode());

        // Fetch room with players to avoid N+1 and to broadcast full list
        Room room = roomRepository.findByRoomCodeWithPlayers(request.getRoomCode())
                .orElseThrow(() -> new RoomExceptions.RoomNotFoundException(request.getRoomCode()));

        // --- Guard: Is the game already running? ---
        if (room.getStatus() != RoomStatus.WAITING) {
            throw new RoomExceptions.RoomNotJoinableException(
                    request.getRoomCode(),
                    "Game is already " + room.getStatus().name().toLowerCase()
            );
        }

        // --- Guard: Is the room full? ---
        if (room.isFull()) {
            throw new RoomExceptions.RoomFullException(request.getRoomCode());
        }

        // --- Reconnect logic: same playerId re-joining? ---
        Optional<Player> existingPlayer = playerRepository
                .findByPlayerIdAndRoom(request.getPlayerId(), room);

        Player player;
        if (existingPlayer.isPresent()) {
            // Player was here before (e.g. refreshed tab). Reconnect them.
            player = existingPlayer.get();
            if (player.getStatus() == PlayerStatus.CONNECTED) {
                // Genuinely already in — not a reconnect, just a duplicate request
                log.warn("Player {} already CONNECTED in room {}, returning current state",
                        request.getPlayerId(), request.getRoomCode());
                return RoomResponse.from(room);
            }
            player.setStatus(PlayerStatus.CONNECTED);
            player.setPlayerName(request.getPlayerName()); // Allow name update on reconnect
            playerRepository.save(player);
            log.info("Player {} reconnected to room {}", request.getPlayerId(), request.getRoomCode());
        } else {
            // New player joining for the first time
            player = Player.builder()
                    .playerId(request.getPlayerId())
                    .playerName(request.getPlayerName())
                    .room(room)
                    .isHost(false)
                    .status(PlayerStatus.CONNECTED)
                    .score(0)
                    .build();

            player = playerRepository.save(player);
            room.getPlayers().add(player);
            log.info("Player {} joined room {} for the first time", request.getPlayerId(), request.getRoomCode());
        }

        broadcastPlayerJoined(room, player);

        return RoomResponse.from(room);
    }

    private void broadcastPlayerJoined(Room room, Player newPlayer) {
        List<PlayerDto> allPlayers = room.getPlayers().stream()
                .filter(p -> p.getStatus() == PlayerStatus.CONNECTED)
                .map(PlayerDto::from)
                .toList();

        LobbyEvent.PlayerJoined event = LobbyEvent.PlayerJoined.builder()
                .event("PLAYER_JOINED")
                .player(PlayerDto.from(newPlayer))
                .players(allPlayers)
                .currentCount(room.getConnectedPlayerCount())
                .maxPlayers(room.getSettings().getMaxPlayers())
                .build();

        messagingTemplate.convertAndSend("/topic/room." + room.getRoomCode(), event);
        log.debug("Broadcast PLAYER_JOINED to /topic/room.{}", room.getRoomCode());
    }

    @Transactional
    public void leaveRoom(String playerId, String roomCode) {
        Room room = roomRepository.findByRoomCodeWithPlayers(roomCode)
                .orElseThrow(() -> new RoomExceptions.RoomNotFoundException(roomCode));

        Player leavingPlayer = playerRepository
                .findByPlayerIdAndRoom(playerId, room)
                .orElse(null);

        if (leavingPlayer == null) {
            log.warn("leaveRoom called for unknown player {} in room {}", playerId, roomCode);
            return;
        }

        leavingPlayer.setStatus(PlayerStatus.DISCONNECTED);
        playerRepository.save(leavingPlayer);

        String newHostPlayerId = null;

        // Promote a new host if the host left
        if (room.getHostPlayerId().equals(playerId)) {
            Optional<Player> newHost = room.getPlayers().stream()
                    .filter(p -> !p.getPlayerId().equals(playerId))
                    .filter(p -> p.getStatus() == PlayerStatus.CONNECTED)
                    .findFirst(); // earliest joiner (list is insertion-ordered)

            if (newHost.isPresent()) {
                newHost.get().setHost(true);
                playerRepository.save(newHost.get());
                room.setHostPlayerId(newHost.get().getPlayerId());
                roomRepository.save(room);
                newHostPlayerId = newHost.get().getPlayerId();
                log.info("Host left room {}. Promoted player {} as new host",
                        roomCode, newHost.get().getPlayerName());
            } else {
                // No players left → mark room as FINISHED
                room.setStatus(RoomStatus.FINISHED);
                roomRepository.save(room);
                log.info("Last player left room {}. Room marked FINISHED.", roomCode);
                return;
            }
        }

        // Broadcast PLAYER_LEFT to remaining clients
        broadcastPlayerLeft(room, leavingPlayer, newHostPlayerId);
    }

    private void broadcastPlayerLeft(Room room, Player leftPlayer, String newHostPlayerId) {
        List<PlayerDto> remainingPlayers = room.getPlayers().stream()
                .filter(p -> p.getStatus() == PlayerStatus.CONNECTED)
                .map(PlayerDto::from)
                .toList();

        LobbyEvent.PlayerLeft event = LobbyEvent.PlayerLeft.builder()
                .event("PLAYER_LEFT")
                .playerId(leftPlayer.getPlayerId())
                .playerName(leftPlayer.getPlayerName())
                .players(remainingPlayers)
                .newHostPlayerId(newHostPlayerId)
                .build();

        messagingTemplate.convertAndSend("/topic/room." + room.getRoomCode(), event);
        log.debug("Broadcast PLAYER_LEFT to /topic/room.{}", room.getRoomCode());
    }

    public List<PublicRoomSummaryResponse> getPublicRooms() {
        return roomRepository
                .findJoinablePublicRooms(RoomType.PUBLIC, RoomStatus.WAITING)
                .stream()
                .map(PublicRoomSummaryResponse::from)
                .toList();
    }

    public RoomResponse getRoom(String roomCode) {
        Room room = roomRepository.findByRoomCodeWithPlayers(roomCode)
                .orElseThrow(() -> new RoomExceptions.RoomNotFoundException(roomCode));
        return RoomResponse.from(room);
    }
}
