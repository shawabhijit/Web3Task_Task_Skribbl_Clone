package com.backend.Utils;

import com.backend.DTO.PlayerDto;
import com.backend.DTO.RoomSettingsDto;
import lombok.Builder;
import lombok.Data;

import java.util.List;

public class LobbyEvent {

    /**
     * Sent when a new player joins — all clients update their player list.
     * Maps to: player_joined -> Server -> Clients
     */
    @Data
    @Builder
    public static class PlayerJoined {
        private String event;         // "PLAYER_JOINED"
        private PlayerDto player;
        private List<PlayerDto> players;
        private int currentCount;
        private int maxPlayers;
    }


    @Data
    @Builder
    public static class PlayerLeft {
        private String event;         // "PLAYER_LEFT"
        private String playerId;
        private String playerName;
        private List<PlayerDto> players;
        private String newHostPlayerId;  // non-null if host was reassigned
    }

    /**
     * Sent to all clients when the room's settings are updated by the host.
     */
    @Data
    @Builder
    public static class SettingsUpdated {
        private String event;         // "SETTINGS_UPDATED"
        private RoomSettingsDto settings;
    }


    @Data
    @Builder
    public static class GameStarting {
        private String event;         // "GAME_STARTING"
        private String roomCode;
    }
}
