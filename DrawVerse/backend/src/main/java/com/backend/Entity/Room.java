package com.backend.Entity;

import com.backend.Entity.Enum.RoomStatus;
import com.backend.Entity.Enum.RoomType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms", indexes = {
        @Index(name = "idx_room_code", columnList = "roomCode", unique = true),
        @Index(name = "idx_room_status_type", columnList = "status, roomType")
})
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String roomCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomStatus status = RoomStatus.WAITING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RoomType roomType = RoomType.PUBLIC;

    @Column(nullable = false)
    private String hostPlayerId;

    @Embedded
    @Builder.Default
    private RoomSettings settings = new RoomSettings();


    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Player> players = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public int getConnectedPlayerCount() {
        return (int) players.stream()
                .filter(p -> p.getStatus() == com.backend.Entity.Enum.PlayerStatus.CONNECTED)
                .count();
    }

    public boolean isFull() {
        return getConnectedPlayerCount() >= settings.getMaxPlayers();
    }

    public boolean isJoinable() {
        return status == RoomStatus.WAITING && !isFull();
    }
}
