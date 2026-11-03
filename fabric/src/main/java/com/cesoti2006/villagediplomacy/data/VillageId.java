package com.cesoti2006.villagediplomacy.data;

import net.minecraft.core.BlockPos;

public final class VillageId {

    public static final int SNAP = 128;

    private VillageId() {
    }

    public static BlockPos snap(BlockPos pos) {
        return new BlockPos((pos.getX() >> 7) << 7, pos.getY(), (pos.getZ() >> 7) << 7);
    }

    public static String of(BlockPos pos) {
        return of(pos.getX(), pos.getZ());
    }

    public static String of(int x, int z) {
        return ((x >> 7) << 7) + "_" + ((z >> 7) << 7);
    }

    public static boolean isLegacy(String id) {
        return id != null && id.startsWith("village_");
    }

    public static BlockPos legacyPos(String legacyId) {
        if (!isLegacy(legacyId)) return null;
        String[] parts = legacyId.substring("village_".length()).split("_");
        if (parts.length != 2) return null;
        try {
            return new BlockPos(Integer.parseInt(parts[0]) << 4, 0, Integer.parseInt(parts[1]) << 4);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
