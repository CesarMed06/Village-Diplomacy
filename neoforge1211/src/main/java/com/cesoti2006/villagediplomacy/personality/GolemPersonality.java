package com.cesoti2006.villagediplomacy.personality;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Random;

public class GolemPersonality {

    public static final ChatFormatting GOLEM_NAME_COLOR = ChatFormatting.AQUA;

    private final String name;
    private final GolemTrait temperament;
    private final GolemTrait loyalty;

    private final String legacyStory;

    private final int storyTemplateIndex;

    public enum GolemTrait {
        GENTLE,
        STERN,
        FIERCE,
        DEVOTED,
        DUTIFUL,
        INDEPENDENT;

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

    public static GolemPersonality generateRandom(Random random) {
        String name = generateGolemName(random);

        GolemTrait temperament = random.nextInt(10) < 3 ? GolemTrait.GENTLE
                : random.nextInt(10) < 7 ? GolemTrait.STERN : GolemTrait.FIERCE;

        GolemTrait loyalty = random.nextInt(10) < 3 ? GolemTrait.DEVOTED
                : random.nextInt(10) < 7 ? GolemTrait.DUTIFUL : GolemTrait.INDEPENDENT;

        int storyIdx = random.nextInt(7);
        return new GolemPersonality(name, temperament, loyalty, "", storyIdx);
    }

    private static final String[] GOLEM_NAME_IDS = {
            "ironheart", "steelguard", "stonefist", "ironwall", "defender",
            "guardian", "sentinel", "protector", "warden", "keeper",
            "strongarm", "ironforge", "steelheart", "stonewall", "ironclad",
            "bulwark", "anvil", "hammer", "shield", "fortress"
    };

    private static final java.util.Set<String> GOLEM_NAME_KEYS =
            new java.util.HashSet<>(java.util.Arrays.asList(GOLEM_NAME_IDS));

    private static String generateGolemName(Random random) {
        return GOLEM_NAME_IDS[random.nextInt(GOLEM_NAME_IDS.length)];
    }

    public Component nameComponent() {
        Component base = name != null && GOLEM_NAME_KEYS.contains(name)
                ? Component.translatable("villagediplomacy.golemname." + name)
                : Component.literal(name != null ? name : "");
        return base.copy().withStyle(GOLEM_NAME_COLOR);
    }

    public Component getGreetingComponent() {
        String k = switch (temperament) {
            case GENTLE -> "villagediplomacy.golem.greet.gentle";
            case STERN -> "villagediplomacy.golem.greet.stern";
            case FIERCE -> "villagediplomacy.golem.greet.fierce";
            case DEVOTED, DUTIFUL, INDEPENDENT -> "villagediplomacy.golem.greet.default";
        };
        return Component.translatable(k, nameComponent());
    }

    public Component getPatrolComponent() {
        String k = switch (loyalty) {
            case DEVOTED -> "villagediplomacy.golem.patrol.devoted";
            case DUTIFUL -> "villagediplomacy.golem.patrol.dutiful";
            case INDEPENDENT -> "villagediplomacy.golem.patrol.independent";
            case GENTLE, STERN, FIERCE -> "villagediplomacy.golem.patrol.default";
        };
        return Component.translatable(k, nameComponent());
    }

    public Component getThreatDetectedComponent() {
        String k = switch (temperament) {
            case GENTLE -> "villagediplomacy.golem.threat.gentle";
            case STERN -> "villagediplomacy.golem.threat.stern";
            case FIERCE -> "villagediplomacy.golem.threat.fierce";
            case DEVOTED, DUTIFUL, INDEPENDENT -> "villagediplomacy.golem.threat.default";
        };
        return Component.translatable(k, nameComponent());
    }

    public Component getLoyaltyLineComponent() {
        String k = switch (loyalty) {
            case DEVOTED -> "villagediplomacy.golem.loyalty_line.devoted";
            case DUTIFUL -> "villagediplomacy.golem.loyalty_line.dutiful";
            case INDEPENDENT -> "villagediplomacy.golem.loyalty_line.independent";
            case GENTLE, STERN, FIERCE -> "villagediplomacy.golem.loyalty_line.default";
        };
        return Component.translatable(k, nameComponent());
    }

    public Component getTemperamentLineComponent() {
        String k = switch (temperament) {
            case GENTLE -> "villagediplomacy.golem.temperament_line.gentle";
            case STERN -> "villagediplomacy.golem.temperament_line.stern";
            case FIERCE -> "villagediplomacy.golem.temperament_line.fierce";
            case DEVOTED, DUTIFUL, INDEPENDENT -> "villagediplomacy.golem.temperament_line.default";
        };
        return Component.translatable(k, nameComponent());
    }

    public Component getCreationStoryComponent(String villageRef) {
        if (storyTemplateIndex >= 0 && storyTemplateIndex < 7) {
            return Component.translatable("villagediplomacy.golem.story." + storyTemplateIndex, villageRef);
        }
        return Component.literal(legacyStory);
    }

    public Component getFullTitleComponent() {
        return Component.translatable("villagediplomacy.golem.title",
                nameComponent(),
                Component.translatable(temperament.traitKey()).withStyle(ChatFormatting.YELLOW));
    }

    public Component getFriendlyHitResponseComponent(int strikes) {
        if (strikes == 1) {
            String k = switch (temperament) {
                case GENTLE -> "villagediplomacy.golem.warn1.gentle";
                case STERN -> "villagediplomacy.golem.warn1.stern";
                case FIERCE -> "villagediplomacy.golem.warn1.fierce";
                default -> "villagediplomacy.golem.warn1.default";
            };
            return Component.translatable(k, nameComponent()).withStyle(net.minecraft.ChatFormatting.GOLD);
        } else if (strikes == 2) {
            String k = switch (temperament) {
                case GENTLE -> "villagediplomacy.golem.warn2.gentle";
                case STERN -> "villagediplomacy.golem.warn2.stern";
                case FIERCE -> "villagediplomacy.golem.warn2.fierce";
                default -> "villagediplomacy.golem.warn2.default";
            };
            return Component.translatable(k, nameComponent()).withStyle(net.minecraft.ChatFormatting.RED);
        } else {
            return Component.translatable("villagediplomacy.golem.warn3", nameComponent())
                    .withStyle(net.minecraft.ChatFormatting.DARK_RED);
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

}
