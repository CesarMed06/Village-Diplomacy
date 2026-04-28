package com.cesoti2006.villagediplomacy.util;

import java.util.Random;

public final class VillageNameGenerator {

    private static final String[] PREFIXES = {
        "Oak", "Stone", "River", "Iron", "Shadow", "Golden", "Silver", "Dark",
        "Bright", "Storm", "Frost", "Ember", "Mossy", "Ancient", "Wild", "Hollow"
    };

    private static final String[] SUFFIXES = {
        "haven", "ford", "stead", "ridge", "vale", "hollow", "gate", "wood",
        "field", "cliff", "moor", "wick", "bridge", "crest", "watch", "keep"
    };

    private VillageNameGenerator() {}

    public static String generateNameFromPosition(int x, int z) {
        long seed = ((long) x * 341873128712L) ^ ((long) z * 132897987541L);
        java.util.Random rng = new java.util.Random(seed);
        int pi = rng.nextInt(PREFIXES.length);
        int si = rng.nextInt(SUFFIXES.length);
        return pi + "|" + si;
    }

    public static int prefixCount() {
        return PREFIXES.length;
    }

    public static int suffixCount() {
        return SUFFIXES.length;
    }

    public static String prefixKey(int index) {
        return "villagediplomacy.village.prefix." + index;
    }

    public static String suffixKey(int index) {
        return "villagediplomacy.village.suffix." + index;
    }

    public static String generateStoredName(Random random) {
        int pi = random.nextInt(PREFIXES.length);
        int si = random.nextInt(SUFFIXES.length);
        return pi + "|" + si;
    }

    public static String getPrefixRaw(int index) {
        return PREFIXES[index];
    }

    public static String getSuffixRaw(int index) {
        return SUFFIXES[index];
    }
}
