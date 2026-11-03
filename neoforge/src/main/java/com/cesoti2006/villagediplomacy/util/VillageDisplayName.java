package com.cesoti2006.villagediplomacy.util;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

public final class VillageDisplayName {

    private static final java.util.Set<String> KNOWN_BIOMES = java.util.Set.of(
        "plains", "desert", "snow", "taiga", "savanna", "jungle", "swamp"
    );

    private VillageDisplayName() {}

    private static String biomeOf(String stored) {
        if (stored == null || !stored.contains(":")) return null;
        String[] parts = stored.split(":", 2);
        String biome = parts[0].trim();
        if (!KNOWN_BIOMES.contains(biome)) return null;
        try {
            Integer.parseInt(parts[1].trim());
            return biome;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Component asComponent(String stored) {
        if (stored == null || stored.isEmpty()) return Component.empty();
        String biome = biomeOf(stored);
        if (biome != null) {
            return Component.translatable(VillageNameGenerator.nameKey(biome,
                    Integer.parseInt(stored.split(":", 2)[1].trim())));
        }
        if (stored.contains("|")) {
            String[] p = stored.split("[|]", 2);
            try {
                int pi = Integer.parseInt(p[0].trim());
                int si = Integer.parseInt(p[1].trim());
                return Component.translatable(
                        "villagediplomacy.village.compound",
                        Component.translatable(VillageNameGenerator.prefixKey(pi)),
                        Component.translatable(VillageNameGenerator.suffixKey(si)));
            } catch (NumberFormatException ignored) {}
        }
        return Component.literal(stored);
    }

    public static String visibleName(String stored) {
        if (stored == null || stored.isEmpty()) return null;
        String biome = biomeOf(stored);
        if (biome != null) {
            int index = Integer.parseInt(stored.split(":", 2)[1].trim());
            String key = VillageNameGenerator.nameKey(biome, index);
            String text = Language.getInstance().getOrDefault(key);
            return key.equals(text) ? null : text;
        }
        String plain = stripFormatting(stored).trim();
        return plain.isEmpty() ? null : plain;
    }

    private static String stripFormatting(String text) {
        StringBuilder cleaned = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()
                    && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(text.charAt(i + 1)) >= 0) {
                i++;
                continue;
            }
            cleaned.append(c);
        }
        return cleaned.toString();
    }
}
