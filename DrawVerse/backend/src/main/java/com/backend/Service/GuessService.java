package com.backend.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuessService {

    /**
     * Compares a player's guess with the actual word (case-insensitive).
     * 
     * @param guess      Player's guess text
     * @param actualWord The actual word to guess
     * @return true if guess matches word (after trim and lowercase)
     */
    public boolean isCorrectGuess(String guess, String actualWord) {
        if (guess == null || actualWord == null) {
            return false;
        }
        return guess.trim().toLowerCase().equals(actualWord.trim().toLowerCase());
    }

    /**
     * Calculates points for a correct guess based on time remaining.
     * Faster guesses = more points.
     * Formula: base points * (timeLeft / totalTime)
     * 
     * @param timeLeft  Seconds remaining on timer
     * @param totalTime Total draw time in seconds
     * @return Points awarded (0-250)
     */
    public int calculateGuesserPoints(int timeLeft, int totalTime) {
        if (timeLeft < 0 || totalTime <= 0) {
            return 0;
        }

        // Base points for guessing: 100
        // Bonus: up to 150 points based on time
        int basePoints = 100;
        int bonusPoints = (int) ((timeLeft * 150) / totalTime);

        return Math.min(basePoints + bonusPoints, 250);
    }

    /**
     * Calculates points for the drawer when someone guesses correctly.
     * Drawer gets points for each correct guess.
     * More players guessing = more points for drawer.
     * 
     * @param correctGuessCount How many players have guessed correctly so far
     * @param totalPlayers      Total non-drawer players in room
     * @return Points for drawer (0-150)
     */
    public int calculateDrawerPoints(int correctGuessCount, int totalPlayers) {
        if (totalPlayers <= 0) {
            return 0;
        }

        // Base points: 50 per correct guess
        // Bonus: up to 100 if everyone guesses correctly
        int basePoints = 50 * correctGuessCount;
        int bonus = (int) ((correctGuessCount * 100) / totalPlayers);

        return Math.min(basePoints + bonus, 150);
    }

    /**
     * Penalty for drawer if no one guesses correctly after full draw time.
     * 
     * @return Negative points (0, no penalty applied - drawer just doesn't get
     *         points)
     */
    public int getDrawerPenalty() {
        return 0; // No penalty, just no points
    }

    /**
     * Calculates time bonus (for early guesses).
     * Players who guess in first 25% of time get extra multiplier.
     * 
     * @param timeLeft  Seconds remaining
     * @param totalTime Total draw time
     * @return Multiplier (1.0x to 1.5x)
     */
    public double getTimeMultiplier(int timeLeft, int totalTime) {
        if (totalTime <= 0)
            return 1.0;

        double percentageLeft = (double) timeLeft / totalTime;

        if (percentageLeft > 0.75) { // In first 25% of time
            return 1.5;
        } else if (percentageLeft > 0.5) { // In first 50% of time
            return 1.25;
        }
        return 1.0;
    }

    /**
     * Summary of points calculation for display.
     */
    public String getPointsBreakdown(int timeLeft, int totalTime, int correctGuesserCount, int totalPlayers,
            boolean isCorrect) {
        if (!isCorrect) {
            return "0 pts - Incorrect guess";
        }

        int points = calculateGuesserPoints(timeLeft, totalTime);
        double multiplier = getTimeMultiplier(timeLeft, totalTime);
        int finalPoints = (int) (points * multiplier);

        if (multiplier > 1.0) {
            return finalPoints + " pts - Bonus for quick guess!";
        }
        return finalPoints + " pts";
    }
}
