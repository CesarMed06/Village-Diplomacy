package com.cesoti2006.villagediplomacy.personality;

import java.util.Random;

public enum PersonalityTrait {

    COWARD,
    CAUTIOUS,
    NEUTRAL_COURAGE,
    BRAVE,
    FEARLESS,

    GREEDY,
    THRIFTY,
    NEUTRAL_GENEROSITY,
    GENEROUS,
    CHARITABLE,

    LAZY,
    RELAXED,
    NEUTRAL_WORK,
    HARDWORKING,
    WORKAHOLIC,

    SHY,
    RESERVED,
    NEUTRAL_SOCIAL,
    OUTGOING,
    EXTROVERTED,

    CALM,
    PATIENT,
    NEUTRAL,
    IRRITABLE,
    HOTHEADED,

    CUNNING,
    SHREWD,
    NEUTRAL_HONESTY,
    HONEST,
    TRUSTWORTHY,

    PESSIMISTIC,
    REALISTIC,
    NEUTRAL_OUTLOOK,
    OPTIMISTIC,
    CHEERFUL;

    public static PersonalityTrait randomCourage(Random random) {
        int roll = random.nextInt(100);
        if (roll < 10) return COWARD;          
        if (roll < 25) return CAUTIOUS;        
        if (roll < 75) return NEUTRAL_COURAGE; 
        if (roll < 90) return BRAVE;           
        return FEARLESS;                        
    }

    public static PersonalityTrait randomGenerosity(Random random) {
        int roll = random.nextInt(100);
        if (roll < 12) return GREEDY;             
        if (roll < 30) return THRIFTY;            
        if (roll < 70) return NEUTRAL_GENEROSITY; 
        if (roll < 88) return GENEROUS;           
        return CHARITABLE;                         
    }

    public static PersonalityTrait randomWorkEthic(Random random) {
        int roll = random.nextInt(100);
        if (roll < 15) return LAZY;         
        if (roll < 35) return RELAXED;      
        if (roll < 65) return NEUTRAL_WORK; 
        if (roll < 85) return HARDWORKING;  
        return WORKAHOLIC;                   
    }

    public static PersonalityTrait randomSocialBehavior(Random random) {
        int roll = random.nextInt(100);
        if (roll < 10) return SHY;            
        if (roll < 30) return RESERVED;       
        if (roll < 70) return NEUTRAL_SOCIAL; 
        if (roll < 90) return OUTGOING;       
        return EXTROVERTED;                    
    }

    public static PersonalityTrait randomTemperament(Random random) {
        int roll = random.nextInt(100);
        if (roll < 15) return CALM;          
        if (roll < 40) return PATIENT;       
        if (roll < 70) return NEUTRAL;
        if (roll < 90) return IRRITABLE;     
        return HOTHEADED;                     
    }

    public static PersonalityTrait randomHonesty(Random random) {
        int roll = random.nextInt(100);
        if (roll < 10) return CUNNING;          
        if (roll < 25) return SHREWD;           
        if (roll < 65) return NEUTRAL_HONESTY;  
        if (roll < 85) return HONEST;           
        return TRUSTWORTHY;                      
    }

    public static PersonalityTrait randomOutlook(Random random) {
        int roll = random.nextInt(100);
        if (roll < 12) return PESSIMISTIC;      
        if (roll < 35) return REALISTIC;        
        if (roll < 65) return NEUTRAL_OUTLOOK;  
        if (roll < 85) return OPTIMISTIC;       
        return CHEERFUL;                         
    }
}
