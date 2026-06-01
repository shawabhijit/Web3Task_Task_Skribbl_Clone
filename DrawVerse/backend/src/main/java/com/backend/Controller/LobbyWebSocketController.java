package com.backend.Controller;

import com.backend.Entity.Room;
import com.backend.Exceptions.RoomExceptions;
import com.backend.Repository.PlayerRepository;
import com.backend.Repository.RoomRepository;
import com.backend.Service.GuessService;
import com.backend.Service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LobbyWebSocketController {

    private final PlayerRepository playerRepository;
    private final RoomRepository roomRepository;
    private final GuessService guessService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;

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

    /**
     * Handle player guess submission during the drawing phase.
     * Validates guess, checks correctness, awards points, and broadcasts result.
     */
    @MessageMapping("/room.{roomCode}.guess")
    public void handleGuess(
            @Payload Map<String, String> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        String roomCodeParam = (String) headerAccessor.getSessionAttributes().get("roomCode");
        if (roomCodeParam == null || roomCodeParam.isBlank()) {
            roomCodeParam = payload.get("roomCode");
        }
        if (roomCodeParam == null || roomCodeParam.isBlank()) {
            log.warn("Guess received without roomCode. payload={}", payload);
            return;
        }
        final String roomCode = roomCodeParam.toUpperCase();

        String playerId = payload.get("playerId");
        String guessText = payload.getOrDefault("guess", "").trim();

        log.info("Guess received in room {}: player {} guessed '{}'", roomCode, playerId, guessText);

        try {
            // Fetch room with players
            Room room = roomRepository.findByRoomCodeWithPlayers(roomCode.toUpperCase())
                    .orElseThrow(() -> new RoomExceptions.RoomNotFoundException(roomCode));

            // Validate game is in drawing phase
            if (!room.getStatus().name().equals("IN_PROGRESS")) {
                messagingTemplate.convertAndSend("/topic/room." + roomCode + ".error",
                        (Object) Map.of("event", "GUESS_INVALID", "reason", "Game is not in progress"));
                return;
            }

            // Validate guess is not empty
            if (guessText.isEmpty()) {
                messagingTemplate.convertAndSend("/topic/room." + roomCode + ".guess_result",
                        (Object) Map.of(
                                "event", "GUESS_INVALID",
                                "playerId", playerId,
                                "reason", "Guess cannot be empty"));
                return;
            }

            // Find the player
            var player = playerRepository.findByPlayerIdAndRoom(playerId, room)
                    .orElseThrow(() -> new RuntimeException("Player not found"));

            // Don't allow drawer to guess
            if (player.getPlayerId().equals(room.getCurrentDrawerId())) {
                messagingTemplate.convertAndSend("/topic/room." + roomCode + ".guess_result",
                        (Object) Map.of(
                                "event", "GUESS_INVALID",
                                "playerId", playerId,
                                "reason", "Drawer cannot guess"));
                return;
            }

            // Check if player already guessed this round
            if (player.isGuessedCorrectly()) {
                messagingTemplate.convertAndSend("/topic/room." + roomCode + ".guess_result",
                        (Object) Map.of(
                                "event", "GUESS_INVALID",
                                "playerId", playerId,
                                "reason", "You already guessed correctly this round"));
                return;
            }

            // Compare guess with actual word
            boolean isCorrect = guessService.isCorrectGuess(guessText, room.getCurrentWord());

            if (isCorrect) {
                // Calculate points based on time remaining
                long elapsedSeconds = Duration.between(
                        room.getGameStartedAt(),
                        LocalDateTime.now()).getSeconds();
                int timeLeft = Math.max(0, room.getSettings().getDrawTimeSeconds() - (int) elapsedSeconds);
                int pointsAwarded = guessService.calculateGuesserPoints(
                        timeLeft,
                        room.getSettings().getDrawTimeSeconds());

                // Award points to guesser
                player.addScore(pointsAwarded);
                player.setGuessedCorrectly(true);
                playerRepository.save(player);

                // Award points to drawer
                long guesserCount = room.getPlayers().stream()
                        .filter(p -> p.isGuessedCorrectly() && !p.getPlayerId().equals(room.getCurrentDrawerId()))
                        .count();

                int drawerPoints = guessService.calculateDrawerPoints(
                        (int) guesserCount + 1,
                        room.getPlayers().size() - 1);

                var drawer = playerRepository.findById(
                        room.getPlayers().stream()
                                .filter(p -> p.getPlayerId().equals(room.getCurrentDrawerId()))
                                .findFirst()
                                .map(p -> p.getId())
                                .orElse(-1L))
                        .orElse(null);

                if (drawer != null) {
                    drawer.addScore(drawerPoints);
                    playerRepository.save(drawer);
                }

                // Broadcast correct guess to all players
                messagingTemplate.convertAndSend("/topic/room." + roomCode + ".guess_result",
                        (Object) Map.of(
                                "event", "GUESS_CORRECT",
                                "playerId", playerId,
                                "playerName", player.getPlayerName(),
                                "pointsAwarded", pointsAwarded,
                                "newScore", player.getScore(),
                                "feedback",
                                "✓ " + player.getPlayerName() + " guessed correctly! +" + pointsAwarded + " pts"));

                log.info("Correct guess in room {}: {} guessed '{}', awarded {} points",
                        roomCode, player.getPlayerName(), guessText, pointsAwarded);

            } else {
                // Incorrect guess - broadcast to all but don't award points
                messagingTemplate.convertAndSend("/topic/room." + roomCode + ".guess_result",
                        (Object) Map.of(
                                "event", "GUESS_INCORRECT",
                                "playerId", playerId,
                                "playerName", player.getPlayerName(),
                                "feedback", player.getPlayerName() + " made a guess..."));

                log.debug("Incorrect guess in room {}: {} guessed '{}'",
                        roomCode, player.getPlayerName(), guessText);
            }

        } catch (Exception e) {
            log.error("Error processing guess in room {}: {}", roomCode, e.getMessage(), e);
            messagingTemplate.convertAndSend("/topic/room." + roomCode + ".error",
                    (Object) Map.of("event", "GUESS_ERROR", "message", "Error processing your guess"));
        }
    }
}
