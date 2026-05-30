package com.backend.exchanges;

import com.backend.Entity.Room;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicRoomSummaryResponse {
    private String roomCode;
    private int currentPlayers;
    private int maxPlayers;
    private int rounds;
    private int drawTimeSeconds;
    private String hostName;

    public static PublicRoomSummaryResponse from(Room room) {
        String hostName = room.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(room.getHostPlayerId()))
                .map(p -> p.getPlayerName())
                .findFirst()
                .orElse("Unknown");

        return PublicRoomSummaryResponse.builder()
                .roomCode(room.getRoomCode())
                .currentPlayers(room.getConnectedPlayerCount())
                .maxPlayers(room.getSettings().getMaxPlayers())
                .rounds(room.getSettings().getRounds())
                .drawTimeSeconds(room.getSettings().getDrawTimeSeconds())
                .hostName(hostName)
                .build();
    }
}
