package com.cesoti2006.villagediplomacy.util;

import java.util.Random;

public class VillageNameGenerator {

    private static final String[] PREFIXES = {
            "Oak", "Stone", "River", "Green", "Silver", "Iron", "Gold", "Willow",
            "Maple", "Pine", "Cedar", "Ash", "Elder", "White", "Black", "Red",
            "Spring", "Summer", "Winter", "Autumn", "Dawn", "Dusk", "Moon", "Sun",
            "North", "South", "East", "West", "High", "Low", "Deep", "Bright"
    };

    private static final String[] SUFFIXES = {
            "shire", "wood", "vale", "field", "brook", "ford", "haven", "ton",
            "burg", "mill", "ridge", "cliff", "crest", "dale", "glen", "hollow",
            "port", "watch", "guard", "mount", "hill", "peak", "shore", "bay",
            "rest", "moor", "marsh", "fen", "grove", "glade", "point", "end"
    };

    public static String generateName(long seed) {
        Random random = new Random(seed);
        String prefix = PREFIXES[random.nextInt(PREFIXES.length)];
        String suffix = SUFFIXES[random.nextInt(SUFFIXES.length)];
        return prefix + suffix;
    }

    public static String generateNameFromPosition(int x, int z) {
        long seed = ((long) x << 32) | (z & 0xFFFFFFFFL);
        return generateName(seed);
    }
}
