package com.cesoti2006.villagediplomacy.personality;

/**
 * Estados emocionales que afectan el comportamiento del aldeano
 */
public enum EmotionalState {
    NEUTRAL,        // Normal
    HAPPY,          // Acaba de comerciar bien, fue salvado, etc.
    SAD,            // Murió un vecino, fue robado
    SCARED,         // Hay enemigos cerca, fue atacado
    ANGRY,          // Le pegaron, le robaron
    GRATEFUL,       // Le salvaste la vida
    MOURNING;       // Está de luto por un aldeano muerto
    
    /**
     * Duración del estado emocional en milisegundos
     */
    public long getDuration() {
        switch (this) {
            case HAPPY: return 60000;      // 1 minuto
            case GRATEFUL: return 300000;  // 5 minutos
            case SCARED: return 120000;    // 2 minutos
            case ANGRY: return 180000;     // 3 minutos
            case SAD: return 240000;       // 4 minutos
            case MOURNING: return 600000;  // 10 minutos
            default: return 0;
        }
    }
    
    /**
     * Partículas que aparecen sobre el aldeano
     */
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
