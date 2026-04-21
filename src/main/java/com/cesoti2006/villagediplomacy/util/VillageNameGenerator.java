package com.cesoti2006.villagediplomacy.util;

import java.util.Random;

public class VillageNameGenerator {

    private static final String[] PREFIX_KEYS = {
            "villagediplomacy.vn.oak", "villagediplomacy.vn.stone", "villagediplomacy.vn.river", "villagediplomacy.vn.green",
            "villagediplomacy.vn.silver", "villagediplomacy.vn.iron", "villagediplomacy.vn.gold", "villagediplomacy.vn.willow",
            "villagediplomacy.vn.maple", "villagediplomacy.vn.pine", "villagediplomacy.vn.cedar", "villagediplomacy.vn.ash",
            "villagediplomacy.vn.elder", "villagediplomacy.vn.white", "villagediplomacy.vn.black", "villagediplomacy.vn.red",
            "villagediplomacy.vn.spring", "villagediplomacy.vn.summer", "villagediplomacy.vn.winter", "villagediplomacy.vn.autumn",
            "villagediplomacy.vn.dawn", "villagediplomacy.vn.dusk", "villagediplomacy.vn.moon", "villagediplomacy.vn.sun",
            "villagediplomacy.vn.north", "villagediplomacy.vn.south", "villagediplomacy.vn.east", "villagediplomacy.vn.west",
            "villagediplomacy.vn.high", "villagediplomacy.vn.low", "villagediplomacy.vn.deep", "villagediplomacy.vn.bright",
            "villagediplomacy.vn.crystal", "villagediplomacy.vn.shadow", "villagediplomacy.vn.dragon", "villagediplomacy.vn.wolf",
            "villagediplomacy.vn.fox", "villagediplomacy.vn.star", "villagediplomacy.vn.mist", "villagediplomacy.vn.frost",
            "villagediplomacy.vn.thunder", "villagediplomacy.vn.blossom", "villagediplomacy.vn.royal", "villagediplomacy.vn.emerald",
            "villagediplomacy.vn.copper", "villagediplomacy.vn.azure", "villagediplomacy.vn.obsidian", "villagediplomacy.vn.amber"
    };

    private static final String[] SUFFIX_KEYS = {
            "villagediplomacy.vn.shire", "villagediplomacy.vn.wood", "villagediplomacy.vn.vale", "villagediplomacy.vn.field",
            "villagediplomacy.vn.brook", "villagediplomacy.vn.ford", "villagediplomacy.vn.haven", "villagediplomacy.vn.ton",
            "villagediplomacy.vn.burg", "villagediplomacy.vn.mill", "villagediplomacy.vn.ridge", "villagediplomacy.vn.cliff",
            "villagediplomacy.vn.crest", "villagediplomacy.vn.dale", "villagediplomacy.vn.glen", "villagediplomacy.vn.hollow",
            "villagediplomacy.vn.port", "villagediplomacy.vn.watch", "villagediplomacy.vn.guard", "villagediplomacy.vn.mount",
            "villagediplomacy.vn.hill", "villagediplomacy.vn.peak", "villagediplomacy.vn.shore", "villagediplomacy.vn.bay",
            "villagediplomacy.vn.rest", "villagediplomacy.vn.moor", "villagediplomacy.vn.marsh", "villagediplomacy.vn.fen",
            "villagediplomacy.vn.grove", "villagediplomacy.vn.glade", "villagediplomacy.vn.point", "villagediplomacy.vn.end",
            "villagediplomacy.vn.fort", "villagediplomacy.vn.market", "villagediplomacy.vn.cross", "villagediplomacy.vn.falls",
            "villagediplomacy.vn.meadow", "villagediplomacy.vn.spire", "villagediplomacy.vn.gate", "villagediplomacy.vn.cove"
    };

    public static int prefixCount() {
        return PREFIX_KEYS.length;
    }

    public static int suffixCount() {
        return SUFFIX_KEYS.length;
    }

    public static String prefixKey(int index) {
        return PREFIX_KEYS[index];
    }

    public static String suffixKey(int index) {
        return SUFFIX_KEYS[index];
    }

    /** Deterministic storage token: {@code prefixIndex|suffixIndex} */
    public static String generateSerializedFromPosition(int x, int z) {
        long seed = ((long) x << 32) | (z & 0xFFFFFFFFL);
        Random random = new Random(seed);
        int pi = random.nextInt(PREFIX_KEYS.length);
        int si = random.nextInt(SUFFIX_KEYS.length);
        return pi + "|" + si;
    }

    /** @deprecated Use {@link #generateSerializedFromPosition(int, int)} for localized names */
    @Deprecated
    public static String generateName(long seed) {
        Random random = new Random(seed);
        return PREFIX_KEYS[random.nextInt(PREFIX_KEYS.length)] + SUFFIX_KEYS[random.nextInt(SUFFIX_KEYS.length)];
    }

    /** @deprecated Use {@link #generateSerializedFromPosition(int, int)} */
    @Deprecated
    public static String generateNameFromPosition(int x, int z) {
        return generateSerializedFromPosition(x, z);
    }
}
