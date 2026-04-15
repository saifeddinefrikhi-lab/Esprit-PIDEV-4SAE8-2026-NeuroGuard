package com.neuroguard.forumsservice.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * Computes readability score (Flesch Reading Ease) for text.
 * Higher score = easier to read. Typical range 0–100.
 */
@Service
public class ReadabilityService {

    private static final Pattern WORD_PATTERN = Pattern.compile("\\b\\w+\\b");
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[.!?]+");

    /**
     * Flesch Reading Ease: 206.835 - 1.015 * (words/sentences) - 84.6 * (syllables/words).
     * Returns a value typically between 0 and 100 (higher = easier).
     */
    public double computeFleschReadingEase(String text) {
        if (text == null || text.isBlank()) return 0;
        String[] words = WORD_PATTERN.matcher(text).results()
                .map(m -> m.group().toLowerCase())
                .toArray(String[]::new);
        int wordCount = words.length;
        if (wordCount == 0) return 0;
        int sentenceCount = Math.max(1, SENTENCE_PATTERN.split(text).length);
        int syllableCount = 0;
        for (String word : words) {
            syllableCount += countSyllables(word);
        }
        double wordsPerSentence = (double) wordCount / sentenceCount;
        double syllablesPerWord = (double) syllableCount / wordCount;
        double score = 206.835 - 1.015 * wordsPerSentence - 84.6 * syllablesPerWord;
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Label for display: Easy (60+), Medium (30–60), Hard (0–30).
     */
    public String getReadabilityLabel(double fleschScore) {
        if (fleschScore >= 60) return "Easy";
        if (fleschScore >= 30) return "Medium";
        return "Hard";
    }

    /** Approximate syllable count by counting vowel groups (a,e,i,o,u,y). */
    private int countSyllables(String word) {
        if (word == null || word.isEmpty()) return 0;
        word = word.toLowerCase();
        int count = 0;
        boolean prevVowel = false;
        for (int i = 0; i < word.length(); i++) {
            boolean isVowel = "aeiouy".indexOf(word.charAt(i)) >= 0;
            if (isVowel && !prevVowel) count++;
            prevVowel = isVowel;
        }
        if (word.endsWith("e") && count > 1) count--;
        return Math.max(1, count);
    }
}
