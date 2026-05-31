package com.backend.Utils;

import com.backend.DTO.PlayerDto;
import com.backend.DTO.RoomSettingsDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

public class LobbyEvent {

    /**
     * Sent when a new player joins — all clients update their player list.
     * Maps to: player_joined -> Server -> Clients
     */
    @Data
    @Builder
    public static class PlayerJoined {
        private String event; // "PLAYER_JOINED"
        private PlayerDto player;
        private List<PlayerDto> players;
        private int currentCount;
        private int maxPlayers;
    }

    @Data
    @Builder
    public static class PlayerLeft {
        private String event; // "PLAYER_LEFT"
        private String playerId;
        private String playerName;
        private List<PlayerDto> players;
        private String newHostPlayerId; // non-null if host was reassigned
    }

    /**
     * Sent to all clients when the room's settings are updated by the host.
     */
    @Data
    @Builder
    public static class SettingsUpdated {
        private String event; // "SETTINGS_UPDATED"
        private RoomSettingsDto settings;
    }

    @Data
    @Builder
    public static class GameStarting {
        private String event; // "GAME_STARTING"
        private String roomCode;
    }

    /**
     * Sent to all clients when the host starts the game.
     * Indicates the game is transitioning from WAITING to IN_PROGRESS.
     */
    @Data
    @Builder
    public static class GameStarted {
        private String event; // "GAME_STARTED"
        private int round; // Current round number
        private int totalRounds; // Total rounds in this game
        private String drawerId; // ID of first drawer
        private int drawTimeSeconds; // How long the drawer has
    }

    /**
     * Sent to all clients when a new round begins.
     * Drawer receives the full word; others receive a blank hint.
     */
    @Data
    @Builder
    public static class RoundStart {
        private String event; // "ROUND_START"
        private int round;
        private String drawerId; // Current drawer
        private String word; // Full word (ONLY sent to drawer, null for others)
        private String hint; // Blank hint (e.g., "_ _ _ _") sent to guessers
        private int drawTimeSeconds;
        private List<PlayerDto> players;
    }

    /**
     * Sent every second during the draw phase to update timer on all clients.
     */
    @Data
    @Builder
    public static class TimerTick {
        private String event; // "TIMER_TICK"
        private int timeLeft; // Seconds remaining
    }

    /**
     * Sent when a round ends (time expired or other trigger).
     * Reveals the word and current scores.
     */
    @Data
    @Builder
    public static class RoundEnd {
        private String event; // "ROUND_END"
        private String word; // Revealed word
        private List<PlayerDto> players; // Updated player list with new scores
        private int round;
        private int totalRounds;
    }

    /**
     * Sent when the game is completely over.
     * Shows final rankings and scores.
     */
    @Data
    @Builder
    public static class GameOver {
        private String event; // "GAME_OVER"
        private List<PlayerDto> finalScores; // Sorted by score descending
        private Map<String, Integer> rankings; // playerId -> ranking (1st, 2nd, etc)
    }
}
