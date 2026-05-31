package com.backend.Utils;

import com.backend.Service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * Manages game timers for each room.
 * Runs a countdown for each round's draw phase and broadcasts TIMER_TICK
 * events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GameTimer {

    private final SimpMessagingTemplate messagingTemplate;
    private final RoomService roomService;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> activeTimers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(10);

    /**
     * Starts a countdown timer for a room's round.
     * Broadcasts TIMER_TICK every second and calls endRound when time expires.
     *
     * @param roomCode        The room code
     * @param drawTimeSeconds The draw time in seconds
     */
    public void startRoundTimer(String roomCode, int drawTimeSeconds) {
        // Cancel any existing timer for this room
        stopRoundTimer(roomCode);

        log.info("Starting {} second timer for room {}", drawTimeSeconds, roomCode);

        final int[] timeLeft = { drawTimeSeconds };

        ScheduledFuture<?> timerFuture = executorService.scheduleAtFixedRate(() -> {
            try {
                timeLeft[0]--;
                roomService.broadcastTimerTick(roomCode, timeLeft[0]);

                if (timeLeft[0] <= 0) {
                    log.info("Timer expired for room {}. Ending round.", roomCode);
                    stopRoundTimer(roomCode);
                    roomService.endRound(roomCode);
                }
            } catch (Exception e) {
                log.error("Error in timer for room {}", roomCode, e);
                stopRoundTimer(roomCode);
            }
        }, 1, 1, TimeUnit.SECONDS);

        activeTimers.put(roomCode, timerFuture);
    }

    /**
     * Stops the running timer for a room.
     *
     * @param roomCode The room code
     */
    public void stopRoundTimer(String roomCode) {
        ScheduledFuture<?> future = activeTimers.remove(roomCode);
        if (future != null) {
            future.cancel(false);
            log.info("Timer stopped for room {}", roomCode);
        }
    }

    /**
     * Stops all active timers (e.g., on shutdown).
     */
    public void stopAll() {
        activeTimers.values().forEach(f -> f.cancel(false));
        activeTimers.clear();
        executorService.shutdown();
        log.info("All timers stopped");
    }
}
