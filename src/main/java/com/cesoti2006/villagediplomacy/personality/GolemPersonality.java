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
        GENTLE("Gentil", "§a"),      // Pacífico, amigable
        STERN("Severo", "§7"),          // Serio, estricto
        FIERCE("Feroz", "§c"),        // Feroz, agresivo
        
        // Lealtad
        DEVOTED("Devoto", "§6"),      // Devoto, protector extremo
        DUTIFUL("Cumplidor", "§e"),      // Cumplidor, hace su trabajo
        INDEPENDENT("Independiente", "§b"); // Independiente, hace lo suyo
        
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
        String[] names = {
            "Ironheart", "Steelguard", "Stonefist", "Ironwall", "Defender",
            "Guardian", "Sentinel", "Protector", "Warden", "Keeper",
            "Strongarm", "Ironforge", "Steelheart", "Stonewall", "Ironclad",
            "Bulwark", "Anvil", "Hammer", "Shield", "Fortress"
        };
        
        return names[random.nextInt(names.length)];
    }
    
    private static String generateCreationStory(String villageName, GolemTrait temperament, 
                                                GolemTrait loyalty, Random random) {
        String[] stories = {
            "Creado durante un asedio zombi para proteger " + villageName,
            "Forjado por el herrero de la aldea en tiempos antiguos",
            "Despertado cuando la aldea estaba en grave peligro",
            "Construido por los ancianos para guardar las puertas de la aldea",
            "Formado de hierro donado por comerciantes agradecidos",
            "Creado durante una gran celebración de paz",
            "Alzado para honrar a un héroe caído de la aldea"
        };
        
        return stories[random.nextInt(stories.length)];
    }
    
    /**
     * Mensaje de saludo según personalidad
     */
    public String getGreetingMessage() {
        return switch (temperament) {
            case GENTLE -> "§a[" + name + "] *asiente pacíficamente*";
            case STERN -> "§7[" + name + "] *permanece en guardia silenciosamente*";
            case FIERCE -> "§c[" + name + "] *pisa fuerte, listo para la batalla*";
            default -> "§7[" + name + "] *mira silenciosamente*";
        };
    }
    
    /**
     * Mensaje de patrullaje
     */
    public String getPatrolMessage() {
        return switch (loyalty) {
            case DEVOTED -> "§6[" + name + "] ¡Nada dañará esta aldea!";
            case DUTIFUL -> "§e[" + name + "] *patrulla el perímetro*";
            case INDEPENDENT -> "§b[" + name + "] *vaga libremente*";
            default -> "§7[" + name + "] *patrulla*";
        };
    }
    
    /**
     * Mensaje cuando ve enemigos
     */
    public String getThreatDetectedMessage() {
        return switch (temperament) {
            case GENTLE -> "§a[" + name + "] ¡Por favor, vete en paz!";
            case STERN -> "§7[" + name + "] No eres bienvenido aquí.";
            case FIERCE -> "§c[" + name + "] *RUGIDO HOSTIL*";
            default -> "§c[" + name + "] *alerta*";
        };
    }
    
    /**
     * Respuesta al ser golpeado por jugador amigable
     */
    public String getFriendlyHitResponse(int strikes) {
        if (strikes == 1) {
            return switch (temperament) {
                case GENTLE -> "§a[" + name + "] Amigo... ¿por qué?";
                case STERN -> "§7[" + name + "] Para. Ahora.";
                case FIERCE -> "§c[" + name + "] *gruñe* ¡No me pongas a prueba!";
                default -> "§e[" + name + "] ¡Hey!";
            };
        } else if (strikes == 2) {
            return switch (temperament) {
                case GENTLE -> "§6[" + name + "] ¡Por favor... no quiero hacerte daño!";
                case STERN -> "§c[" + name + "] Esta es tu última advertencia.";
                case FIERCE -> "§4[" + name + "] ¡ESTÁS COMETIENDO UN ERROR!";
                default -> "§c[" + name + "] ¡Para!";
            };
        } else {
            return "§4[" + name + "] ¡Que así sea!";
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
