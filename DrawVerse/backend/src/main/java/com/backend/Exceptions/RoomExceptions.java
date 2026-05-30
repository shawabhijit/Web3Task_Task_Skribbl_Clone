package com.backend.Exceptions;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class RoomExceptions {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    public static class RoomNotFoundException extends RuntimeException {
        public RoomNotFoundException(String roomCode) {
            super("Room not found with code: " + roomCode);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class RoomFullException extends RuntimeException {
        public RoomFullException(String roomCode) {
            super("Room " + roomCode + " is full");
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class RoomNotJoinableException extends RuntimeException {
        public RoomNotJoinableException(String roomCode, String reason) {
            super("Room " + roomCode + " cannot be joined: " + reason);
        }
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    public static class PlayerAlreadyInRoomException extends RuntimeException {
        public PlayerAlreadyInRoomException(String playerId, String roomCode) {
            super("Player " + playerId + " is already in room " + roomCode);
        }
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    public static class NotHostException extends RuntimeException {
        public NotHostException() {
            super("Only the host can perform this action");
        }
    }
}
