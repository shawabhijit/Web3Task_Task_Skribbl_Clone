package com.backend.Utils;

import java.util.Arrays;
import java.util.List;

/**
 * Hard-coded word lists for the Skribbl.io clone game.
 * Words are categorized by difficulty level and game mode.
 */
public class WordList {

    public static final List<String> NORMAL_WORDS = Arrays.asList(
            // Animals
            "dog", "cat", "elephant", "giraffe", "lion", "tiger", "monkey", "bear",
            "rabbit", "penguin", "duck", "eagle", "fish", "whale", "shark", "octopus",

            // Food
            "pizza", "burger", "sandwich", "spaghetti", "apple", "banana", "orange",
            "strawberry", "chocolate", "ice cream", "cookie", "bread", "cheese", "egg",

            // Objects
            "house", "car", "bicycle", "airplane", "boat", "rocket", "computer", "phone",
            "camera", "watch", "glasses", "shoe", "hat", "book", "pen", "pencil",

            // Nature
            "tree", "mountain", "river", "ocean", "beach", "desert", "forest", "valley",
            "volcano", "island", "lake", "waterfall", "cloud", "sun", "moon", "star",

            // Sports
            "football", "basketball", "tennis", "baseball", "cricket", "golf", "swimming",
            "running", "skiing", "ice skating", "boxing", "wrestling",

            // Activities
            "dancing", "singing", "painting", "cooking", "reading", "writing", "sleeping",
            "playing", "laughing", "crying", "walking", "jumping", "climbing", "swimming",

            // Emotions
            "happy", "sad", "angry", "surprised", "excited", "scared", "bored", "confused",

            // Weather
            "rain", "snow", "wind", "thunder", "lightning", "fog", "hail",

            // Body Parts
            "head", "hand", "foot", "eye", "nose", "ear", "mouth", "tooth", "arm", "leg",

            // Common Objects
            "table", "chair", "door", "window", "light", "mirror", "cup", "plate",
            "fork", "knife", "spoon", "bed", "pillow", "blanket", "sofa");

    public static final List<String> HARD_WORDS = Arrays.asList(
            // Challenging Animals
            "platypus", "armadillo", "hippopotamus", "rhinoceros", "chameleon",

            // Complex Objects
            "microscope", "telescope", "skateboard", "trampoline", "accordion",

            // Abstract
            "democracy", "revolution", "evolution", "atmosphere", "photography",
            "archaeology", "mythology", "psychology", "philosophy", "technology",

            // Difficult Concepts
            "metaphor", "palindrome", "labyrinth", "paradox", "silhouette",
            "skeleton", "hieroglyphic", "kaleidoscope", "silhouette", "symmetry");

    public static final List<String> MIXED_WORDS = Arrays.asList(
            // Mix of easy and hard
            "apple", "architecture", "dog", "algorithm", "tree", "telescope",
            "pizza", "photography", "car", "carousel", "house", "hierarchy",
            "flower", "fluorescent", "guitar", "galaxy", "cloud", "kaleidoscope");

    /**
     * Get a random word from the specified word mode.
     * Defaults to NORMAL mode if unknown mode is specified.
     *
     * @param wordMode The game mode (NORMAL, HARD, MIXED)
     * @return A random word from the appropriate list
     */
    public static String getRandomWord(String wordMode) {
        List<String> words = switch (wordMode != null ? wordMode.toUpperCase() : "NORMAL") {
            case "HARD" -> HARD_WORDS;
            case "MIXED" -> MIXED_WORDS;
            default -> NORMAL_WORDS;
        };

        int randomIndex = (int) (Math.random() * words.size());
        return words.get(randomIndex);
    }

    /**
     * Get all words for a specific mode.
     */
    public static List<String> getWordsByMode(String wordMode) {
        return switch (wordMode != null ? wordMode.toUpperCase() : "NORMAL") {
            case "HARD" -> HARD_WORDS;
            case "MIXED" -> MIXED_WORDS;
            default -> NORMAL_WORDS;
        };
    }
}
