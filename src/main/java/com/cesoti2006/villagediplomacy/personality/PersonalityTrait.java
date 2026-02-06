package com.cesoti2006.villagediplomacy.personality;

import java.util.Random;

/**
 * Personality traits a villager can have (EXPANDED SYSTEM)
 */
public enum PersonalityTrait {
    // Courage (affects combat behavior)
    COWARD,
    CAUTIOUS,
    NEUTRAL_COURAGE,
    BRAVE,
    FEARLESS,
    
    // Generosity (affects trading and gifts)
    GREEDY,
    THRIFTY,
    NEUTRAL_GENEROSITY,
    GENEROUS,
    CHARITABLE,
    
    // Work ethic (affects work speed and dedication)
    LAZY,
    RELAXED,
    NEUTRAL_WORK,
    HARDWORKING,
    WORKAHOLIC,
    
    // Social behavior (affects interactions)
    SHY,
    RESERVED,
    NEUTRAL_SOCIAL,
    OUTGOING,
    EXTROVERTED,
    
    // Temperament (affects mood changes)
    CALM,
    PATIENT,
    NEUTRAL,
    IRRITABLE,
    HOTHEADED,
    
    // Honesty (affects prices and deals)
    CUNNING,
    SHREWD,
    NEUTRAL_HONESTY,
    HONEST,
    TRUSTWORTHY,
    
    // Outlook (affects particle effects and reactions)
    PESSIMISTIC,
    REALISTIC,
    NEUTRAL_OUTLOOK,
    OPTIMISTIC,
    CHEERFUL;
    
    public static PersonalityTrait randomCourage(Random random) {
        int roll = random.nextInt(100);
        if (roll < 10) return COWARD;          // 10% cowards
        if (roll < 25) return CAUTIOUS;        // 15% cautious
        if (roll < 75) return NEUTRAL_COURAGE; // 50% neutral
        if (roll < 90) return BRAVE;           // 15% brave
        return FEARLESS;                        // 10% fearless
    }
    
    public static PersonalityTrait randomGenerosity(Random random) {
        int roll = random.nextInt(100);
        if (roll < 12) return GREEDY;             // 12% greedy
        if (roll < 30) return THRIFTY;            // 18% thrifty
        if (roll < 70) return NEUTRAL_GENEROSITY; // 40% neutral
        if (roll < 88) return GENEROUS;           // 18% generous
        return CHARITABLE;                         // 12% charitable
    }
    
    public static PersonalityTrait randomWorkEthic(Random random) {
        int roll = random.nextInt(100);
        if (roll < 15) return LAZY;         // 15% lazy
        if (roll < 35) return RELAXED;      // 20% relaxed
        if (roll < 65) return NEUTRAL_WORK; // 30% neutral
        if (roll < 85) return HARDWORKING;  // 20% hardworking
        return WORKAHOLIC;                   // 15% workaholic
    }
    
    public static PersonalityTrait randomSocialBehavior(Random random) {
        int roll = random.nextInt(100);
        if (roll < 10) return SHY;            // 10% shy
        if (roll < 30) return RESERVED;       // 20% reserved
        if (roll < 70) return NEUTRAL_SOCIAL; // 40% neutral
        if (roll < 90) return OUTGOING;       // 20% outgoing
        return EXTROVERTED;                    // 10% extroverted
    }
    
    public static PersonalityTrait randomTemperament(Random random) {
        int roll = random.nextInt(100);
        if (roll < 15) return CALM;          // 15% calm
        if (roll < 40) return PATIENT;       // 25% patient
        if (roll < 70) return NEUTRAL;// 30% neutral
        if (roll < 90) return IRRITABLE;     // 20% irritable
        return HOTHEADED;                     // 10% hotheaded
    }
    
    public static PersonalityTrait randomHonesty(Random random) {
        int roll = random.nextInt(100);
        if (roll < 10) return CUNNING;          // 10% cunning
        if (roll < 25) return SHREWD;           // 15% shrewd
        if (roll < 65) return NEUTRAL_HONESTY;  // 40% neutral
        if (roll < 85) return HONEST;           // 20% honest
        return TRUSTWORTHY;                      // 15% trustworthy
    }
    
    public static PersonalityTrait randomOutlook(Random random) {
        int roll = random.nextInt(100);
        if (roll < 12) return PESSIMISTIC;      // 12% pessimistic
        if (roll < 35) return REALISTIC;        // 23% realistic
        if (roll < 65) return NEUTRAL_OUTLOOK;  // 30% neutral
        if (roll < 85) return OPTIMISTIC;       // 20% optimistic
        return CHEERFUL;                         // 15% cheerful
    }
}
