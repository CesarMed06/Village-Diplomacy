package com.cesoti2006.villagediplomacy.util;

import net.minecraft.network.chat.Component;

public final class VillageDisplayName {

    private VillageDisplayName() {
    }

    /**
     * Stored form {@code prefixIndex|suffixIndex} uses translated fragments; legacy plain strings     * (e.g. old worlds) show as-is.
     */
    public static Component asComponent(String stored) {
        if (stored == null || stored.isEmpty()) {
            return Component.empty();
        }
        if (stored.contains("|")) {
            String[] p = stored.split("\\|", 2);
            try {
                int pi = Integer.parseInt(p[0].trim());
                int si = Integer.parseInt(p[1].trim());
                if (pi >= 0 && pi < VillageNameGenerator.prefixCount() && si >= 0 && si < VillageNameGenerator.suffixCount()) {
                    return Component.translatable(
                            "villagediplomacy.village.compound",
                            Component.translatable(VillageNameGenerator.prefixKey(pi)),
                            Component.translatable(VillageNameGenerator.suffixKey(si)));
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return Component.literal(stored);
    }
}
