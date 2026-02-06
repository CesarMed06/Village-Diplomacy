package com.cesoti2006.villagediplomacy.personality;

import java.util.Random;

/**
 * Personalidad de un Iron Golem
 */
public class GolemPersonality {
    private final String name;
    private final GolemTrait temperament;
    private final GolemTrait loyalty;
    private final String creationStory;
    
    public enum GolemTrait {
        // Temperamento
        GENTLE("Gentle", "§a"),      // Pacífico, amigable
        STERN("Stern", "§7"),          // Serio, estricto
        FIERCE("Fierce", "§c"),        // Feroz, agresivo
        
        // Lealtad
        DEVOTED("Devoted", "§6"),      // Devoto, protector extremo
        DUTIFUL("Dutiful", "§e"),      // Cumplidor, hace su trabajo
        INDEPENDENT("Independent", "§b"); // Independiente, hace lo suyo
        
        private final String displayName;
        private final String color;
        
        GolemTrait(String displayName, String color) {
            this.displayName = displayName;
            this.color = color;
        }
        
        public String getDisplayName() { return displayName; }
        public String getColor() { return color; }
    }
    
    public GolemPersonality(String name, GolemTrait temperament, GolemTrait loyalty, String creationStory) {
        this.name = name;
        this.temperament = temperament;
        this.loyalty = loyalty;
        this.creationStory = creationStory;
    }
    
    /**
     * Generar personalidad aleatoria para un golem
     */
    public static GolemPersonality generateRandom(String villageName, Random random) {
        String name = generateGolemName(random);
        
        GolemTrait temperament = random.nextInt(10) < 3 ? GolemTrait.GENTLE :
                                 random.nextInt(10) < 7 ? GolemTrait.STERN :
                                 GolemTrait.FIERCE;
        
        GolemTrait loyalty = random.nextInt(10) < 3 ? GolemTrait.DEVOTED :
                            random.nextInt(10) < 7 ? GolemTrait.DUTIFUL :
                            GolemTrait.INDEPENDENT;
        
        String story = generateCreationStory(villageName, temperament, loyalty, random);
        
        return new GolemPersonality(name, temperament, loyalty, story);
    }
    
    private static String generateGolemName(Random random) {
        String[] prefixes = {
            "Iron", "Steel", "Bronze", "Stone", "Guardian",
            "Sentinel", "Warden", "Protector", "Defender", "Keeper"
        };
        
        String[] suffixes = {
            "fist", "heart", "shield", "wall", "guard",
            "watch", "stand", "forge", "anvil", "hammer"
        };
        
        return prefixes[random.nextInt(prefixes.length)] + 
               suffixes[random.nextInt(suffixes.length)];
    }
    
    private static String generateCreationStory(String villageName, GolemTrait temperament, 
                                                GolemTrait loyalty, Random random) {
        String[] stories = {
            "Created during a zombie siege to protect " + villageName,
            "Forged by the village blacksmith in ancient times",
            "Awakened when the village was in grave danger",
            "Built by the elders to guard the village gates",
            "Formed from iron donated by grateful traders",
            "Crafted during a great celebration of peace",
            "Raised to honor a fallen village hero"
        };
        
        return stories[random.nextInt(stories.length)];
    }
    
    /**
     * Mensaje de saludo según personalidad
     */
    public String getGreetingMessage() {
        return switch (temperament) {
            case GENTLE -> "§a[" + name + "] *nods peacefully*";
            case STERN -> "§7[" + name + "] *stands watch silently*";
            case FIERCE -> "§c[" + name + "] *stomps ground, ready for battle*";
            default -> "§7[" + name + "] *stares silently*";
        };
    }
    
    /**
     * Mensaje de patrullaje
     */
    public String getPatrolMessage() {
        return switch (loyalty) {
            case DEVOTED -> "§6[" + name + "] Nothing will harm this village!";
            case DUTIFUL -> "§e[" + name + "] *patrols the perimeter*";
            case INDEPENDENT -> "§b[" + name + "] *wanders freely*";
            default -> "§7[" + name + "] *patrols*";
        };
    }
    
    /**
     * Mensaje cuando ve enemigos
     */
    public String getThreatDetectedMessage() {
        return switch (temperament) {
            case GENTLE -> "§a[" + name + "] Please, leave peacefully!";
            case STERN -> "§7[" + name + "] You're not welcome here.";
            case FIERCE -> "§c[" + name + "] *HOSTILE ROAR*";
            default -> "§c[" + name + "] *alert*";
        };
    }
    
    /**
     * Respuesta al ser golpeado por jugador amigable
     */
    public String getFriendlyHitResponse(int strikes) {
        if (strikes == 1) {
            return switch (temperament) {
                case GENTLE -> "§a[" + name + "] Friend... why?";
                case STERN -> "§7[" + name + "] Stop. Now.";
                case FIERCE -> "§c[" + name + "] *growls* Don't test me!";
                default -> "§e[" + name + "] Hey!";
            };
        } else if (strikes == 2) {
            return switch (temperament) {
                case GENTLE -> "§6[" + name + "] Please... I don't want to hurt you!";
                case STERN -> "§c[" + name + "] This is your final warning.";
                case FIERCE -> "§4[" + name + "] YOU'RE MAKING A MISTAKE!";
                default -> "§c[" + name + "] Stop!";
            };
        } else {
            return "§4[" + name + "] So be it!";
        }
    }
    
    // Getters
    public String getName() { return name; }
    public String getFullTitle() { 
        return temperament.getColor() + name + " §7(" + temperament.getDisplayName() + ")"; 
    }
    public GolemTrait getTemperament() { return temperament; }
    public GolemTrait getLoyalty() { return loyalty; }
    public String getCreationStory() { return creationStory; }
}
