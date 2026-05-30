package com.backend.DTO;

import com.backend.Entity.Player;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlayerDto {
    private String playerId;
    private String playerName;
    private int score;
    private boolean isHost;
    private String status;

    public static PlayerDto from(Player p) {
        return PlayerDto.builder()
                .playerId(p.getPlayerId())
                .playerName(p.getPlayerName())
                .score(p.getScore())
                .isHost(p.isHost())
                .status(p.getStatus().name())
                .build();
    }
}
