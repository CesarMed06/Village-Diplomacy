package com.cesoti2006.villagediplomacy.data;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class VillageContext {

    public static final int DEFAULT_VANILLA_RADIUS = 80;

    public enum Type {
        VANILLA,
        CLAIMED
    }

    private final String id;
    private final BlockPos center;
    private final ResourceKey<Level> dimension;
    private final String name;
    private final int radius;
    private final Type type;

    public VillageContext(String id, BlockPos center, ResourceKey<Level> dimension,
                          String name, int radius, Type type) {
        this.id = id;
        this.center = center;
        this.dimension = dimension;
        this.name = name;
        this.radius = radius;
        this.type = type;
    }

    public String id() {
        return id;
    }

    public BlockPos center() {
        return center;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public String name() {
        return name;
    }

    public int radius() {
        return radius;
    }

    public Type type() {
        return type;
    }

    public boolean isClaimed() {
        return type == Type.CLAIMED;
    }

    public boolean contains(BlockPos pos) {
        double dx = pos.getX() - center.getX();
        double dz = pos.getZ() - center.getZ();
        return dx * dx + dz * dz <= (double) radius * radius;
    }

    public static VillageContext of(ServerLevel level, BlockPos pos) {
        VillageRelationshipData relationships = VillageRelationshipData.get(level);
        String id = VillageId.of(pos);

        BlockPos center = relationships.getVillagePosition(id);
        if (center == null) center = VillageId.snap(pos);
        String name = relationships.getVillageName(id);

        PlayerClaimedVillageData claimed = PlayerClaimedVillageData.get(level);
        PlayerClaimedVillageData.ClaimedVillage claim = claimed.getVillageAt(center);
        if (claim != null) {
            return new VillageContext(id, claim.center, level.dimension(), claim.name,
                    claim.radius, Type.CLAIMED);
        }

        return new VillageContext(id, center, level.dimension(), name,
                DEFAULT_VANILLA_RADIUS, Type.VANILLA);
    }
}
