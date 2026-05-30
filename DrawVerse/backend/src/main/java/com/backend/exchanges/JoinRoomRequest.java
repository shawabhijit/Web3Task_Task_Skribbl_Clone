package com.backend.exchanges;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JoinRoomRequest {
    @NotBlank(message = "Player name is required")
    @Size(min = 2, max = 30, message = "Player name must be 2–30 characters")
    private String playerName;

    @NotBlank(message = "Player ID is required")
    private String playerId;

    @NotBlank(message = "Room code is required")
    @Size(min = 6, max = 6, message = "Room code must be 6 characters")
    private String roomCode;
}
