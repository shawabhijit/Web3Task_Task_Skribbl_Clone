package com.backend.Entity;

import com.backend.Entity.Enum.PlayerStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "players", indexes = {
        @Index(name = "idx_player_id_room", columnList = "playerId, room_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Client-generated UUID. The browser generates this once and stores it in
     * localStorage so reconnects can reclaim the same player slot.
     */
    @Column(nullable = false, length = 36)
    private String playerId;

    @Column(nullable = false, length = 30)
    private String playerName;

    /**
     * Current WebSocket session ID. Updated on reconnect. Used for targeted events.
     */
    @Column(length = 100)
    private String socketSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PlayerStatus status = PlayerStatus.CONNECTED;

    @Builder.Default
    private int score = 0;

    @Builder.Default
    private boolean isHost = false;

    /**
     * Whether this player has guessed the current word correctly this round.
     * Reset each round by the game engine.
     */
    @Builder.Default
    private boolean guessedCorrectly = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime joinedAt;

    public void addScore(int points) {
        this.score += points;
    }

    public void resetRoundState() {
        this.guessedCorrectly = false;
    }
}
