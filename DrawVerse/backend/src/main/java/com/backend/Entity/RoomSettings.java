package com.backend.Entity;


import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Embeddable room settings — all host-configurable values from the spec.
 * Stored as columns on the rooms table (no join needed).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomSettings {

    @Builder.Default
    private int maxPlayers = 8;

    @Builder.Default
    private int rounds = 3;

    @Builder.Default
    private int drawTimeSeconds = 80;

    @Builder.Default
    private int wordCount = 3;


    @Builder.Default
    private int hints = 2;

    @Builder.Default
    private String wordMode = "NORMAL";
}
