package com.cesoti2006.villagediplomacy.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.Random;

public final class VillageNameGenerator {

    public static final String[] BIOMES = {
        "plains", "desert", "snow", "taiga", "savanna", "jungle", "swamp"
    };

    private static final int POOL = 16;

    private VillageNameGenerator() {}

    public static String detectBiome(ServerLevel level, BlockPos pos) {
        Holder<Biome> holder = level.getBiome(pos);
        ResourceLocation key = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getKey(holder.value());
        if (key == null) return "plains";
        String path = key.getPath();
        if (path.contains("desert") || path.contains("badlands") || path.contains("mesa")) return "desert";
        if (path.contains("snow") || path.contains("frozen") || path.contains("ice") || path.contains("tundra")) return "snow";
        if (path.contains("taiga") || path.contains("spruce") || path.contains("old_growth_pine")) return "taiga";
        if (path.contains("savanna") || path.contains("windswept_savanna")) return "savanna";
        if (path.contains("jungle") || path.contains("bamboo")) return "jungle";
        if (path.contains("swamp") || path.contains("mangrove")) return "swamp";
        return "plains";
    }

    public static String generateFromBiome(ServerLevel level, BlockPos pos) {
        String biome = detectBiome(level, pos);
        long seed = ((long) pos.getX() * 341873128712L) ^ ((long) pos.getZ() * 132897987541L);
        Random rng = new Random(seed);
        int index = rng.nextInt(POOL);
        return biome + ":" + index;
    }

    public static String generateNameFromPosition(int x, int z) {
        long seed = ((long) x * 341873128712L) ^ ((long) z * 132897987541L);
        Random rng = new Random(seed);
        int index = rng.nextInt(POOL);
        return "plains:" + index;
    }

    public static int prefixCount() { return 0; }
    public static int suffixCount() { return 0; }
    public static String prefixKey(int i) { return ""; }
    public static String suffixKey(int i) { return ""; }
    public static String getPrefixRaw(int i) { return ""; }
    public static String getSuffixRaw(int i) { return ""; }
    public static String generateStoredName(Random random) {
        return "plains:" + random.nextInt(POOL);
    }

    public static String nameKey(String biome, int index) {
        return "villagediplomacy.village.name." + biome + "." + index;
    }
}
