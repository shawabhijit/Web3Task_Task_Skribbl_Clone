package com.backend.Controller;

import com.backend.Service.RoomService;
import com.backend.exchanges.CreateRoomRequest;
import com.backend.exchanges.JoinRoomRequest;
import com.backend.exchanges.PublicRoomSummaryResponse;
import com.backend.exchanges.RoomResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Slf4j
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        log.info("POST /api/v1/rooms - Creating room, type: {}", request.getRoomType());
        RoomResponse response = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/join")
    public ResponseEntity<RoomResponse> joinRoom(@Valid @RequestBody JoinRoomRequest request) {
        log.info("POST /api/v1/rooms/join - Player joining room: {}", request.getRoomCode());
        RoomResponse response = roomService.joinRoom(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{roomCode}")
    public ResponseEntity<RoomResponse> getRoom(@PathVariable String roomCode) {
        return ResponseEntity.ok(roomService.getRoom(roomCode.toUpperCase()));
    }

    @GetMapping("/public")
    public ResponseEntity<List<PublicRoomSummaryResponse>> getPublicRooms() {
        return ResponseEntity.ok(roomService.getPublicRooms());
    }

    @DeleteMapping("/{roomCode}/leave")
    public ResponseEntity<Void> leaveRoom(
            @PathVariable String roomCode,
            @RequestParam String playerId) {
        roomService.leaveRoom(playerId, roomCode.toUpperCase());
        return ResponseEntity.noContent().build();
    }

    /**
     * Host starts the game. Transitions from WAITING to IN_PROGRESS,
     * selects first drawer, and initiates round countdown.
     */
    @PostMapping("/{roomCode}/start")
    public ResponseEntity<RoomResponse> startGame(
            @PathVariable String roomCode,
            @RequestParam String playerId) {
        log.info("POST /api/v1/rooms/{}/start - Player {} starting game", roomCode, playerId);
        RoomResponse response = roomService.startGame(roomCode.toUpperCase(), playerId);
        return ResponseEntity.ok(response);
    }
}
