package com.cesoti2006.villagediplomacy.personality;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Random;

/**
 * Personalidad de un Iron Golem
 */
public class GolemPersonality {
    private final String name;
    private final GolemTrait temperament;
    private final GolemTrait loyalty;
    /** Texto de historia guardado en mundos antiguos; vacío si se usa plantilla i18n. */
    private final String legacyStory;
    /** 0–6 = clave villagediplomacy.golem.story.{i}; -1 = usar legacyStory. */
    private final int storyTemplateIndex;

    public enum GolemTrait {
        GENTLE(ChatFormatting.GREEN),
        STERN(ChatFormatting.GRAY),
        FIERCE(ChatFormatting.RED),
        DEVOTED(ChatFormatting.GOLD),
        DUTIFUL(ChatFormatting.YELLOW),
        INDEPENDENT(ChatFormatting.AQUA);

        private final ChatFormatting chatColor;

        GolemTrait(ChatFormatting chatColor) {
            this.chatColor = chatColor;
        }

        public ChatFormatting chatColor() {
            return chatColor;
        }

        public String traitKey() {
            return "villagediplomacy.golem.trait." + name().toLowerCase();
        }
    }

    public GolemPersonality(String name, GolemTrait temperament, GolemTrait loyalty, String legacyStory,
            int storyTemplateIndex) {
        this.name = name;
        this.temperament = temperament;
        this.loyalty = loyalty;
        this.legacyStory = legacyStory != null ? legacyStory : "";
        this.storyTemplateIndex = storyTemplateIndex;
    }

    /**
     * Generar personalidad aleatoria para un golem
     */
    public static GolemPersonality generateRandom(String villageName, Random random) {
        String name = generateGolemName(random);

        GolemTrait temperament = random.nextInt(10) < 3 ? GolemTrait.GENTLE
                : random.nextInt(10) < 7 ? GolemTrait.STERN : GolemTrait.FIERCE;

        GolemTrait loyalty = random.nextInt(10) < 3 ? GolemTrait.DEVOTED
                : random.nextInt(10) < 7 ? GolemTrait.DUTIFUL : GolemTrait.INDEPENDENT;

        int storyIdx = random.nextInt(7);
        return new GolemPersonality(name, temperament, loyalty, "", storyIdx);
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

    public Component getGreetingComponent() {
        String k = switch (temperament) {
            case GENTLE -> "villagediplomacy.golem.greet.gentle";
            case STERN -> "villagediplomacy.golem.greet.stern";
            case FIERCE -> "villagediplomacy.golem.greet.fierce";
            case DEVOTED, DUTIFUL, INDEPENDENT -> "villagediplomacy.golem.greet.default";
        };
        return Component.translatable(k, name);
    }

    public Component getPatrolComponent() {
        String k = switch (loyalty) {
            case DEVOTED -> "villagediplomacy.golem.patrol.devoted";
            case DUTIFUL -> "villagediplomacy.golem.patrol.dutiful";
            case INDEPENDENT -> "villagediplomacy.golem.patrol.independent";
            case GENTLE, STERN, FIERCE -> "villagediplomacy.golem.patrol.default";
        };
        return Component.translatable(k, name);
    }

    public Component getThreatDetectedComponent() {
        String k = switch (temperament) {
            case GENTLE -> "villagediplomacy.golem.threat.gentle";
            case STERN -> "villagediplomacy.golem.threat.stern";
            case FIERCE -> "villagediplomacy.golem.threat.fierce";
            case DEVOTED, DUTIFUL, INDEPENDENT -> "villagediplomacy.golem.threat.default";
        };
        return Component.translatable(k, name);
    }

    public Component getLoyaltyLineComponent() {
        String k = switch (loyalty) {
            case DEVOTED -> "villagediplomacy.golem.loyalty_line.devoted";
            case DUTIFUL -> "villagediplomacy.golem.loyalty_line.dutiful";
            case INDEPENDENT -> "villagediplomacy.golem.loyalty_line.independent";
            case GENTLE, STERN, FIERCE -> "villagediplomacy.golem.loyalty_line.default";
        };
        return Component.translatable(k, name);
    }

    public Component getTemperamentLineComponent() {
        String k = switch (temperament) {
            case GENTLE -> "villagediplomacy.golem.temperament_line.gentle";
            case STERN -> "villagediplomacy.golem.temperament_line.stern";
            case FIERCE -> "villagediplomacy.golem.temperament_line.fierce";
            case DEVOTED, DUTIFUL, INDEPENDENT -> "villagediplomacy.golem.temperament_line.default";
        };
        return Component.translatable(k, name);
    }

    /**
     * Historia de creación (plantilla i18n con %s = id de aldea, o texto legacy).
     */
    public Component getCreationStoryComponent(String villageRef) {
        if (storyTemplateIndex >= 0 && storyTemplateIndex < 7) {
            return Component.translatable("villagediplomacy.golem.story." + storyTemplateIndex, villageRef);
        }
        return Component.literal(legacyStory);
    }

    public Component getFullTitleComponent() {
        MutableComponent nameC = Component.literal(name).withStyle(temperament.chatColor());
        Component traitC = Component.translatable(temperament.traitKey());
        return Component.translatable("villagediplomacy.golem.title", nameC, traitC);
    }

    /**
     * @deprecated Usar {@link #getFullTitleComponent()} para nombres localizados en la entidad.
     */
    @Deprecated
    public String getFullTitle() {
        return getFullTitleComponent().getString();
    }

    /**
     * Respuesta al ser golpeado por jugador amigable (reservado; claves i18n listas si se conecta al sistema de golpes).
     */
    public String getFriendlyHitResponse(int strikes) {
        if (strikes == 1) {
            return switch (temperament) {
                case GENTLE -> "§a[" + name + "] Amigo... ¿por qué?";
                case STERN -> "§7[" + name + "] Para. Ahora.";
                case FIERCE -> "§c[" + name + "] *gruñe* ¡No me pongas a prueba!";
                case DEVOTED, DUTIFUL, INDEPENDENT -> "§e[" + name + "] ¡Hey!";
            };
        } else if (strikes == 2) {
            return switch (temperament) {
                case GENTLE -> "§6[" + name + "] ¡Por favor... no quiero hacerte daño!";
                case STERN -> "§c[" + name + "] Esta es tu última advertencia.";
                case FIERCE -> "§4[" + name + "] ¡ESTÁS COMETIENDO UN ERROR!";
                case DEVOTED, DUTIFUL, INDEPENDENT -> "§c[" + name + "] ¡Para!";
            };
        } else {
            return "§4[" + name + "] ¡Que así sea!";
        }
    }

    public String getName() {
        return name;
    }

    public GolemTrait getTemperament() {
        return temperament;
    }

    public GolemTrait getLoyalty() {
        return loyalty;
    }

    public int getStoryTemplateIndex() {
        return storyTemplateIndex;
    }

    public String getLegacyStory() {
        return legacyStory;
    }

    /**
     * Solo para compatibilidad con datos antiguos guardados en "Story".
     */
    @Deprecated
    public String getCreationStory() {
        return legacyStory;
    }
}
