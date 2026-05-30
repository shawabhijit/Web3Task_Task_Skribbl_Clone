package com.backend.exchanges;

import com.backend.DTO.PlayerDto;
import com.backend.DTO.RoomSettingsDto;
import com.backend.Entity.Enum.RoomStatus;
import com.backend.Entity.Enum.RoomType;
import com.backend.Entity.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomResponse {
    private Long id;
    private String roomCode;
    private RoomStatus status;
    private RoomType roomType;
    private String hostPlayerId;
    private RoomSettingsDto settings;
    private List<PlayerDto> players;

    public static RoomResponse from(Room room) {
        List<PlayerDto> playerDtos = room.getPlayers().stream()
                .map(PlayerDto::from)
                .toList();

        return RoomResponse.builder()
                .id(room.getId())
                .roomCode(room.getRoomCode())
                .status(room.getStatus())
                .roomType(room.getRoomType())
                .hostPlayerId(room.getHostPlayerId())
                .settings(RoomSettingsDto.from(room.getSettings()))
                .players(playerDtos)
                .build();
    }
}
