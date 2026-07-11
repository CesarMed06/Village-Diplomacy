package com.cesoti2006.villagediplomacy.util;

import net.minecraft.network.chat.Component;

public final class VillageDisplayName {

    private static final java.util.Set<String> KNOWN_BIOMES = java.util.Set.of(
        "plains", "desert", "snow", "taiga", "savanna", "jungle", "swamp"
    );

    private VillageDisplayName() {}

    public static Component asComponent(String stored) {
        if (stored == null || stored.isEmpty()) return Component.empty();
        if (stored.contains(":")) {
            String[] p = stored.split(":", 2);
            String biome = p[0].trim();
            if (KNOWN_BIOMES.contains(biome)) {
                try {
                    int index = Integer.parseInt(p[1].trim());
                    return Component.translatable(VillageNameGenerator.nameKey(biome, index));
                } catch (NumberFormatException ignored) {}
            }
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
}
