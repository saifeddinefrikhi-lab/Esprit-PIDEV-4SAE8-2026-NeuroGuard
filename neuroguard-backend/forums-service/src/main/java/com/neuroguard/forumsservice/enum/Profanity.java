package com.neuroguard.forumsservice.constant;

import lombok.Getter;

@Getter
public enum Profanity {
    // Insults & General
    DAMN("damn"),
    HELL("hell"),
    CRAP("crap"),
    STUPID("stupid"),
    IDIOT("idiot"),
    DUMB("dumb"),
    SUCK("suck"),
    SUCKS("sucks"),
    HATE("hate"),
    KILL("kill"),
    DIE("die"),
    UGLY("ugly"),
    FAT("fat"),
    LOSER("loser"),
    SHUT_UP("shut up"),
    SHUTUP("shutup"),
    
    // Slang & Abbreviations
    WTF("wtf"),
    OMG("omg"),
    BS("bs"),
    SCREW("screw"),
    SCREWED("screwed"),
    FREAKING("freaking"),
    FRICKING("fricking"),
    FRIGGING("frigging"),
    
    // Stronger Language (Categorized for future scalability)
    BLOODY("bloody"),
    BUGGER("bugger"),
    ARSE("arse"),
    ASS("ass"),
    BITCH("bitch"),
    BASTARD("bastard"),
    DICK("dick"),
    COCK("cock"),
    PRICK("prick"),
    PUSSY("pussy"),
    SLUT("slut"),
    WHORE("whore"),
    FAG("fag"),
    RETARD("retard"),
    RETARDED("retarded"),
    NIGGER("nigger"),
    NIGGA("nigga"),
    FUCK("fuck"),
    FUCKING("fucking"),
    FUCKED("fucked"),
    FUCKER("fucker"),
    SHIT("shit"),
    SHITTY("shitty"),
    BULLSHIT("bullshit"),
    DIPSHIT("dipshit"),
    DIP_STICK("dip stick");

    private final String word;

    Profanity(String word) {
        this.word = word;
    }
}
