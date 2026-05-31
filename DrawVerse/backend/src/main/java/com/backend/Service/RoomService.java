package com.backend.Service;

import com.backend.DTO.PlayerDto;
import com.backend.Entity.Enum.PlayerStatus;
import com.backend.Entity.Enum.RoomStatus;
import com.backend.Entity.Enum.RoomType;
import com.backend.Entity.Player;
import com.backend.Exceptions.RoomExceptions;
import com.backend.Repository.PlayerRepository;
import com.backend.Utils.GameTimer;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

        private final RoomRepository roomRepository;
        private final PlayerRepository playerRepository;
        private final RoomCodeGenerator codeGenerator;
        private final WordService wordService;
        private final GameTimer gameTimer;
        private final SimpMessagingTemplate messagingTemplate;

        /**
         * @Transactional ensures the room + player are saved atomically.
         *                If player creation fails, the room is rolled back too.
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
                                        "Game is already " + room.getStatus().name().toLowerCase());
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
                        log.info("Player {} joined room {} for the first time", request.getPlayerId(),
                                        request.getRoomCode());
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

        /**
         * Host initiates game start. Transitions room from WAITING to IN_PROGRESS,
         * selects the first drawer, and broadcasts GAME_STARTED event.
         *
         * @param roomCode The room code
         * @param playerId The player starting the game (must be host)
         * @return Updated RoomResponse
         */
        @Transactional
        public RoomResponse startGame(String roomCode, String playerId) {
                Room room = roomRepository.findByRoomCodeWithPlayers(roomCode)
                                .orElseThrow(() -> new RoomExceptions.RoomNotFoundException(roomCode));

                // Guard: Only host can start
                if (!room.getHostPlayerId().equals(playerId)) {
                        throw new RoomExceptions.NotHostException();
                }

                // Guard: Game must not already be started
                if (room.getStatus() != RoomStatus.WAITING) {
                        throw new RoomExceptions.RoomNotJoinableException(
                                        roomCode,
                                        "Game is already " + room.getStatus().name().toLowerCase());
                }

                // Guard: Minimum 2 players required
                List<Player> connectedPlayers = room.getPlayers().stream()
                                .filter(p -> p.getStatus() == PlayerStatus.CONNECTED)
                                .collect(Collectors.toList());

                if (connectedPlayers.size() < 2) {
                        throw new IllegalArgumentException("At least 2 players required to start game");
                }

                // Transition to IN_PROGRESS and initialize game state
                room.setStatus(RoomStatus.IN_PROGRESS);
                room.setCurrentRound(1);
                room.setGameStartedAt(LocalDateTime.now());

                // Select first drawer (host by default, or first connected player)
                Player firstDrawer = connectedPlayers.stream()
                                .filter(p -> p.getPlayerId().equals(room.getHostPlayerId()))
                                .findFirst()
                                .orElse(connectedPlayers.get(0));

                room.setCurrentDrawerId(firstDrawer.getPlayerId());
                roomRepository.save(room);

                log.info("Game started in room {} (code: {}). First drawer: {}",
                                room.getId(), roomCode, firstDrawer.getPlayerName());

                // Broadcast GAME_STARTED to all clients
                broadcastGameStarted(room);

                // Start the first round (after a brief delay if needed)
                startNewRound(room);

                return RoomResponse.from(room);
        }

        /**
         * Starts a new round: select drawer, choose word, broadcast round start event,
         * and initiate timer countdown.
         */
        @Transactional
        public void startNewRound(Room room) {
                String roomCode = room.getRoomCode();
                room = roomRepository.findByRoomCodeWithPlayers(roomCode)
                                .orElseThrow(() -> new RoomExceptions.RoomNotFoundException(roomCode));

                List<Player> connectedPlayers = room.getPlayers().stream()
                                .filter(p -> p.getStatus() == PlayerStatus.CONNECTED)
                                .collect(Collectors.toList());

                // Select next drawer (rotate through players, skip if already drew this round)
                Optional<Player> nextDrawer = connectedPlayers.stream()
                                .filter(p -> !p.isHasDrawn())
                                .findFirst();

                Player currentDrawer;
                if (nextDrawer.isPresent()) {
                        currentDrawer = nextDrawer.get();
                } else {
                        // All players drew this round, reset hasDrawn and start over
                        connectedPlayers.forEach(p -> p.setHasDrawn(false));
                        playerRepository.saveAll(connectedPlayers);
                        currentDrawer = connectedPlayers.get(0);
                }

                // Select random word
                String word = wordService.getRandomWord(room.getSettings().getWordMode());
                String hint = wordService.getHint(word);

                room.setCurrentDrawerId(currentDrawer.getPlayerId());
                room.setCurrentWord(word);
                roomRepository.save(room);

                log.info("Round {} started in room {}. Drawer: {}, Word: {}",
                                room.getCurrentRound(), room.getRoomCode(), currentDrawer.getPlayerName(), word);

                // Broadcast ROUND_START with word to drawer, hint to others
                broadcastRoundStart(room, currentDrawer, word, hint);

                // Start the draw time countdown
                gameTimer.startRoundTimer(room.getRoomCode(), room.getSettings().getDrawTimeSeconds());
        }

        /**
         * Ends the current round, checks if game should continue or end.
         */
        @Transactional
        public void endRound(String roomCode) {
                // Stop the timer for this room
                gameTimer.stopRoundTimer(roomCode);

                Room room = roomRepository.findByRoomCodeWithPlayers(roomCode)
                                .orElseThrow(() -> new RoomExceptions.RoomNotFoundException(roomCode));

                // Mark current drawer as having drawn this round
                Optional<Player> currentDrawer = room.getPlayers().stream()
                                .filter(p -> p.getPlayerId().equals(room.getCurrentDrawerId()))
                                .findFirst();

                currentDrawer.ifPresent(p -> {
                        p.setHasDrawn(true);
                        playerRepository.save(p);
                });

                // Reset all players' guessState for next round
                room.getPlayers().forEach(Player::resetRoundState);
                playerRepository.saveAll(room.getPlayers());

                log.info("Round {} ended in room {}", room.getCurrentRound(), roomCode);

                // Broadcast ROUND_END with word reveal and scores
                broadcastRoundEnd(room);

                // Check if there are more rounds
                if (room.getCurrentRound() < room.getSettings().getRounds()) {
                        room.setCurrentRound(room.getCurrentRound() + 1);
                        roomRepository.save(room);

                        // Start next round after a delay
                        Thread delayedStart = new Thread(() -> {
                                try {
                                        Thread.sleep(3000); // 3 second delay before next round
                                        startNewRound(room);
                                } catch (InterruptedException e) {
                                        Thread.currentThread().interrupt();
                                        log.error("Interrupted while waiting for next round", e);
                                }
                        });
                        delayedStart.setDaemon(true);
                        delayedStart.start();
                } else {
                        // Game is over
                        endGame(roomCode);
                }
        }

        /**
         * Ends the game, calculates rankings, broadcasts GAME_OVER.
         */
        @Transactional
        public void endGame(String roomCode) {
                Room room = roomRepository.findByRoomCodeWithPlayers(roomCode)
                                .orElseThrow(() -> new RoomExceptions.RoomNotFoundException(roomCode));

                room.setStatus(RoomStatus.FINISHED);
                roomRepository.save(room);

                log.info("Game over in room {}", roomCode);

                broadcastGameOver(room);
        }

        // ==================== WebSocket Broadcasts ====================

        private void broadcastGameStarted(Room room) {
                LobbyEvent.GameStarted event = LobbyEvent.GameStarted.builder()
                                .event("GAME_STARTED")
                                .round(room.getCurrentRound())
                                .totalRounds(room.getSettings().getRounds())
                                .drawerId(room.getCurrentDrawerId())
                                .drawTimeSeconds(room.getSettings().getDrawTimeSeconds())
                                .build();

                messagingTemplate.convertAndSend("/topic/room." + room.getRoomCode(), event);
                log.debug("Broadcast GAME_STARTED to /topic/room.{}", room.getRoomCode());
        }

        private void broadcastRoundStart(Room room, Player drawer, String word, String hint) {
                List<PlayerDto> playerList = room.getPlayers().stream()
                                .filter(p -> p.getStatus() == PlayerStatus.CONNECTED)
                                .map(PlayerDto::from)
                                .toList();

                LobbyEvent.RoundStart event = LobbyEvent.RoundStart.builder()
                                .event("ROUND_START")
                                .round(room.getCurrentRound())
                                .drawerId(drawer.getPlayerId())
                                .word(word) // Full word sent to drawer
                                .hint(hint) // Blank hint sent to guessers
                                .drawTimeSeconds(room.getSettings().getDrawTimeSeconds())
                                .players(playerList)
                                .build();

                messagingTemplate.convertAndSend("/topic/room." + room.getRoomCode(), event);
                log.debug("Broadcast ROUND_START to /topic/room.{}", room.getRoomCode());
        }

        public void broadcastTimerTick(String roomCode, int timeLeft) {
                LobbyEvent.TimerTick event = LobbyEvent.TimerTick.builder()
                                .event("TIMER_TICK")
                                .timeLeft(timeLeft)
                                .build();

                messagingTemplate.convertAndSend("/topic/room." + roomCode, event);
        }

        private void broadcastRoundEnd(Room room) {
                List<PlayerDto> playerList = room.getPlayers().stream()
                                .filter(p -> p.getStatus() == PlayerStatus.CONNECTED)
                                .map(PlayerDto::from)
                                .toList();

                LobbyEvent.RoundEnd event = LobbyEvent.RoundEnd.builder()
                                .event("ROUND_END")
                                .word(room.getCurrentWord())
                                .players(playerList)
                                .round(room.getCurrentRound())
                                .totalRounds(room.getSettings().getRounds())
                                .build();

                messagingTemplate.convertAndSend("/topic/room." + room.getRoomCode(), event);
                log.debug("Broadcast ROUND_END to /topic/room.{}", room.getRoomCode());
        }

        private void broadcastGameOver(Room room) {
                List<PlayerDto> sortedPlayers = room.getPlayers().stream()
                                .filter(p -> p.getStatus() == PlayerStatus.CONNECTED)
                                .map(PlayerDto::from)
                                .sorted((a, b) -> Integer.compare(b.getScore(), a.getScore()))
                                .toList();

                LobbyEvent.GameOver event = LobbyEvent.GameOver.builder()
                                .event("GAME_OVER")
                                .finalScores(sortedPlayers)
                                .build();

                messagingTemplate.convertAndSend("/topic/room." + room.getRoomCode(), event);
                log.debug("Broadcast GAME_OVER to /topic/room.{}", room.getRoomCode());
        }
}
