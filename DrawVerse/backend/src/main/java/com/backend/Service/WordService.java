package com.backend.Service;

import com.backend.Utils.WordList;
import org.springframework.stereotype.Service;

/**
 * Service for managing word selection in the game.
 * Uses hard-coded word lists categorized by difficulty/mode.
 */
@Service
public class WordService {

    /**
     * Get a random word based on the game mode.
     *
     * @param wordMode The game mode (NORMAL, HARD, MIXED). Defaults to NORMAL if
     *                 null.
     * @return A random word from the appropriate word list
     */
    public String getRandomWord(String wordMode) {
        return WordList.getRandomWord(wordMode);
    }

    /**
     * Get a hint string for a word.
     * Returns underscores for each letter (e.g., "_ _ _ _ _" for 5-letter word).
     *
     * @param word The word to generate a hint for
     * @return A hint string with underscores (e.g., "_ _ _ _" for "test")
     */
    public String getHint(String word) {
        if (word == null || word.isEmpty()) {
            return "";
        }

        // For each character in the word, show underscore (with spaces for readability)
        StringBuilder hint = new StringBuilder();
        for (int i = 0; i < word.length(); i++) {
            if (i > 0)
                hint.append(" ");
            // Show spaces as spaces, hide letters with underscores
            if (word.charAt(i) == ' ') {
                hint.append(" ");
            } else {
                hint.append("_");
            }
        }
        return hint.toString();
    }

    /**
     * Reveal a hint by showing some letters.
     * Shows first and last letter, and every nth letter based on word length.
     *
     * @param word        The word to reveal
     * @param revealCount How many hints have been used (for progressive reveal)
     * @return Partially revealed hint
     */
    public String getProgressiveHint(String word, int revealCount) {
        if (word == null || word.isEmpty()) {
            return "";
        }

        char[] hint = new char[word.length()];

        // Always show first and last letter
        hint[0] = word.charAt(0);
        if (word.length() > 1) {
            hint[word.length() - 1] = word.charAt(word.length() - 1);
        }

        // Show more letters based on reveal count
        int step = Math.max(1, word.length() / (revealCount + 2));
        for (int i = 0; i < word.length(); i += step) {
            hint[i] = word.charAt(i);
        }

        // Build result with spaces and underscores
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < hint.length; i++) {
            if (i > 0)
                result.append(" ");
            if (word.charAt(i) == ' ') {
                result.append(" ");
            } else if (hint[i] != '\0') {
                result.append(hint[i]);
            } else {
                result.append("_");
            }
        }

        return result.toString();
    }
}
