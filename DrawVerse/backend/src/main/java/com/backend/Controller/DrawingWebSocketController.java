package com.backend.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class DrawingWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room.{roomCode}.draw")
    public void handleDraw(
            @DestinationVariable String roomCode,
            @Payload Map<String, Object> payload,
            SimpMessageHeaderAccessor headerAccessor) {

        log.debug("DRAW from session {} in room {}", headerAccessor.getSessionId(), roomCode);

        // Add event type so the React client knows how to handle it
        payload.put("event", "DRAW_DATA");

        messagingTemplate.convertAndSend("/topic/room." + roomCode, (Object) payload);
    }

    @MessageMapping("/room.{roomCode}.canvas_clear")
    public void handleCanvasClear(
            @DestinationVariable String roomCode,
            SimpMessageHeaderAccessor headerAccessor) {

        log.debug("CANVAS_CLEAR from session {} in room {}", headerAccessor.getSessionId(), roomCode);

        messagingTemplate.convertAndSend("/topic/room." + roomCode,
                (Object) Map.of("event", "CANVAS_CLEAR"));
    }

    @MessageMapping("/room.{roomCode}.draw_undo")
    public void handleDrawUndo(
            @DestinationVariable String roomCode,
            SimpMessageHeaderAccessor headerAccessor) {

        log.debug("DRAW_UNDO from session {} in room {}", headerAccessor.getSessionId(), roomCode);

        messagingTemplate.convertAndSend("/topic/room." + roomCode,
                (Object) Map.of("event", "DRAW_UNDO"));
    }
}
