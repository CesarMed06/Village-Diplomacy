package com.cesoti2006.villagediplomacy.personality;


public enum EmotionalState {
    NEUTRAL,        
    HAPPY,          
    SAD,            
    SCARED,         
    ANGRY,          
    GRATEFUL,       
    MOURNING;       
    
    
    public long getDuration() {
        switch (this) {
            case HAPPY: return 60000;      
            case GRATEFUL: return 300000;  
            case SCARED: return 120000;    
            case ANGRY: return 180000;     
            case SAD: return 240000;       
            case MOURNING: return 600000;  
            default: return 0;
        }
    }
    
    
    public String getParticleEffect() {
        switch (this) {
            case HAPPY: return "heart";
            case GRATEFUL: return "heart";
            case SCARED: return "cloud";
            case ANGRY: return "angry_villager";
            case SAD: return "rain";
            case MOURNING: return "rain";
            default: return null;
        }
    }
}
