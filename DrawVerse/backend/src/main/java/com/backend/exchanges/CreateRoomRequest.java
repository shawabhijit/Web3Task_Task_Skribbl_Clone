package com.backend.exchanges;

import com.backend.Entity.Enum.RoomType;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateRoomRequest {

    @NotBlank(message = "Player name is required")
    @Size(min = 2, max = 30, message = "Player name must be 2–30 characters")
    private String playerName;

    /**
     * Client-generated UUID. The browser creates this once and persists it
     * in localStorage so the same player can reconnect and reclaim their slot.
     */
    @NotBlank(message = "Player ID is required")
    private String playerId;

    @NotNull(message = "Room type is required")
    private RoomType roomType;

    // ---- Room settings with validation matching the spec ----

    @Min(value = 2, message = "Max players must be at least 2")
    @Max(value = 20, message = "Max players cannot exceed 20")
    private int maxPlayers = 8;

    @Min(value = 2, message = "Rounds must be at least 2")
    @Max(value = 10, message = "Rounds cannot exceed 10")
    private int rounds = 3;

    @Min(value = 15, message = "Draw time must be at least 15 seconds")
    @Max(value = 240, message = "Draw time cannot exceed 240 seconds")
    private int drawTimeSeconds = 80;

    @Min(value = 1, message = "Word count must be at least 1")
    @Max(value = 5, message = "Word count cannot exceed 5")
    private int wordCount = 3;

    @Min(value = 0, message = "Hints cannot be negative")
    @Max(value = 5, message = "Hints cannot exceed 5")
    private int hints = 2;

    private String wordMode = "NORMAL";
}
