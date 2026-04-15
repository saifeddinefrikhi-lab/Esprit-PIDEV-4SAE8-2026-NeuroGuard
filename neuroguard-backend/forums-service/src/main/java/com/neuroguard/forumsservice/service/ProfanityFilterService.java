package com.neuroguard.forumsservice.service;

import lombok.Getter;
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

    @Getter
    public enum Profanity {
        DAMN("damn"), HELL("hell"), CRAP("crap"), STUPID("stupid"), IDIOT("idiot"), DUMB("dumb"),
        SUCK("suck"), SUCKS("sucks"), HATE("hate"), KILL("kill"), DIE("die"), UGLY("ugly"),
        FAT("fat"), LOSER("loser"), SHUT_UP("shut up"), SHUTUP("shutup"), WTF("wtf"),
        OMG("omg"), BS("bs"), SCREW("screw"), SCREWED("screwed"), FREAKING("freaking"),
        FRICKING("fricking"), FRIGGING("frigging"), BLOODY("bloody"), BUGGER("bugger"),
        ARSE("arse"), ASS("ass"), BITCH("bitch"), BASTARD("bastard"), DICK("dick"),
        COCK("cock"), PRICK("prick"), PUSSY("pussy"), SLUT("slut"), WHORE("whore"),
        FAG("fag"), RETARD("retard"), RETARDED("retarded"), NIGGER("nigger"),
        NIGGA("nigga"), FUCK("fuck"), FUCKING("fucking"), FUCKED("fucked"),
        FUCKER("fucker"), SHIT("shit"), SHITTY("shitty"), BULLSHIT("bullshit"),
        DIPSHIT("dipshit"), DIP_STICK("dip stick");

        private final String word;
        Profanity(String word) { this.word = word; }
    }

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
