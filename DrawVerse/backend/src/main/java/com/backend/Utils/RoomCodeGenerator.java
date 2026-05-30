package com.backend.Utils;

import com.backend.Repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
@RequiredArgsConstructor
public class RoomCodeGenerator {
    private static final String CHARACTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // No 0,O,1,I (ambiguous)
    private static final int CODE_LENGTH = 6;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom random = new SecureRandom();
    private final RoomRepository roomRepository;

    public String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String code = generateCode();
            if (!roomRepository.existsByRoomCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Failed to generate unique room code after " + MAX_ATTEMPTS + " attempts");
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
