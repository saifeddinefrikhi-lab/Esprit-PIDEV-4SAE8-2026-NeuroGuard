package com.neuroguard.forumsservice.service;

import com.neuroguard.forumsservice.constant.Profanity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates text for banned/profanity words. Rejects content that contains them.
 */
@Service
public class ProfanityFilterService {

    private static final Set<String> BANNED_WORDS = Arrays.stream(Profanity.values())
            .map(Profanity::getWord)
            .collect(Collectors.toSet());

    private static final String REJECTION_MESSAGE =
            "Your content contains language that is not allowed. Please remove inappropriate words.";

    /**
     * Validates that the text does not contain any banned word (case-insensitive).
     * Uses word-boundary matching so words are detected even with punctuation or odd spacing.
     * @throws ResponseStatusException 400 if any banned word is found
     */
    public void validate(String text) {
        if (text == null || text.isBlank()) return;
        // Normalize: lowercase, keep only letters and spaces (punctuation/numbers become spaces)
        String normalized = text.toLowerCase().replaceAll("[^a-z\\s]", " ");
        // Ensure we can match words at start/end by wrapping in spaces
        String toSearch = " " + normalized + " ";
        for (String word : BANNED_WORDS) {
            if (word.isEmpty()) continue;
            // Whole-word match: space before and after, or at start/end
            String pattern = " " + word + " ";
            if (toSearch.contains(pattern)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, REJECTION_MESSAGE);
            }
        }
    }
}
