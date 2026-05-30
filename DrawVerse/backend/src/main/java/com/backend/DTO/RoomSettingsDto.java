package com.backend.DTO;

import com.backend.Entity.RoomSettings;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoomSettingsDto {

    private int maxPlayers;
    private int rounds;
    private int drawTimeSeconds;
    private int wordCount;
    private int hints;
    private String wordMode;

    public static RoomSettingsDto from(RoomSettings s) {
        return RoomSettingsDto.builder()
                .maxPlayers(s.getMaxPlayers())
                .rounds(s.getRounds())
                .drawTimeSeconds(s.getDrawTimeSeconds())
                .wordCount(s.getWordCount())
                .hints(s.getHints())
                .wordMode(s.getWordMode())
                .build();
    }
}
