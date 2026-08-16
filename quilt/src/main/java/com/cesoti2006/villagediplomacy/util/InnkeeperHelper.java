package com.cesoti2006.villagediplomacy.util;

import com.cesoti2006.villagediplomacy.data.VillageRelationshipData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class InnkeeperHelper {

    public static final int INN_RADIUS = 16;

    private static final int SEARCH_RADIUS = 64;

    private static final int ANCHOR_SCAN_RADIUS = 48;

    private static final int ANCHOR_SCAN_VERTICAL = 8;

    private static final BlockPos NO_BEDS = new BlockPos(0, Integer.MIN_VALUE, 0);

    private static final ConcurrentHashMap<BlockPos, BlockPos> anchorCache = new ConcurrentHashMap<>();

    private InnkeeperHelper() {
    }

    public static Villager findInnkeeper(ServerLevel level, BlockPos villagePos) {
        List<Villager> villagers = level.getEntitiesOfClass(Villager.class,
                new AABB(villagePos).inflate(SEARCH_RADIUS, 128, SEARCH_RADIUS),
                v -> !v.isBaby() && !v.isRemoved() && v.isAlive());
        if (villagers.isEmpty()) return null;

        VillageRelationshipData relData = VillageRelationshipData.get(level);
        String villageId = relData.getVillageId(villagePos);
        UUID stored = relData.getInnkeeperUUID(villageId);

        if (stored != null) {
            for (Villager v : villagers) {
                if (v.getUUID().equals(stored)) return v;
            }
        }

        Villager best = selectBestInnkeeper(villagers, level, villagePos);
        if (best != null) {
            relData.setInnkeeperUUID(villageId, best.getUUID());
        }
        return best;
    }

    private static Villager selectBestInnkeeper(List<Villager> villagers,
                                                  ServerLevel level, BlockPos villagePos) {
        Villager best = null;
        double bestDist = Double.MAX_VALUE;
        int bestLevel = -1;
        UUID bestUuid = null;

        for (Villager v : villagers) {
            BlockPos home = getHomeBed(level, v);
            double dist = (home != null) ? home.distSqr(villagePos) : Double.MAX_VALUE;
            int lvl = v.getVillagerData().getLevel();
            UUID uuid = v.getUUID();

            boolean better = dist < bestDist
                    || (dist == bestDist && lvl > bestLevel)
                    || (dist == bestDist && lvl == bestLevel && bestUuid != null
                        && uuid.compareTo(bestUuid) < 0);
            if (better) {
                bestDist = dist;
                bestLevel = lvl;
                bestUuid = uuid;
                best = v;
            }
        }
        return best;
    }

    public static BlockPos getInnAnchor(ServerLevel level, BlockPos villagePos) {

        BlockPos cached = anchorCache.get(villagePos);
        if (cached != null) {
            if (cached == NO_BEDS) return null;

            if (level.isLoaded(cached) && level.getBlockState(cached).getBlock() instanceof BedBlock) {
                return cached;
            }
            anchorCache.remove(villagePos);
        }

        int cx = villagePos.getX();
        int cz = villagePos.getZ();
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz);

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;

        for (int dx = -ANCHOR_SCAN_RADIUS; dx <= ANCHOR_SCAN_RADIUS; dx++) {
            for (int dz = -ANCHOR_SCAN_RADIUS; dz <= ANCHOR_SCAN_RADIUS; dz++) {
                for (int dy = -ANCHOR_SCAN_VERTICAL; dy <= ANCHOR_SCAN_VERTICAL; dy++) {
                    mutable.set(cx + dx, surfaceY + dy, cz + dz);
                    if (!level.isLoaded(mutable)) continue;
                    if (level.getBlockState(mutable).getBlock() instanceof BedBlock) {
                        double dist = mutable.distSqr(villagePos);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = mutable.immutable();
                        }
                    }
                }
            }
        }

        anchorCache.put(villagePos, best != null ? best : NO_BEDS);
        return best;
    }

    public static boolean isInnBed(ServerLevel level, BlockPos bedPos, BlockPos villagePos) {
        BlockPos anchor = getInnAnchor(level, villagePos);
        if (anchor == null) return false;
        return anchor.distSqr(bedPos) <= (double) (INN_RADIUS * INN_RADIUS);
    }

    private static BlockPos getHomeBed(ServerLevel level, Villager villager) {
        Optional<GlobalPos> home = villager.getBrain().getMemory(MemoryModuleType.HOME);
        if (home.isEmpty()) return null;
        if (!home.get().dimension().equals(level.dimension())) return null;

        BlockPos pos = home.get().pos();
        if (!level.isLoaded(pos)) return null;
        if (!(level.getBlockState(pos).getBlock() instanceof BedBlock)) return null;
        return pos;
    }
}
