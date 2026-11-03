package com.cesoti2006.villagediplomacy.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.levelgen.structure.Structure;

public class VillageDetector {

    private static final Map<String, CachedVillage> villageCache = new HashMap<>();
    private static final Map<String, CachedLevel> villageLevelCache = new HashMap<>();
    private static final long CACHE_TTL_MS = 5000;
    private static final int VILLAGE_LEVEL_RANGE = 20;

    private static class CachedVillage {
        final Optional<BlockPos> villagePos;
        final long timestamp;
        CachedVillage(Optional<BlockPos> pos, long time) {
            this.villagePos = pos;
            this.timestamp = time;
        }
    }

    private static class CachedLevel {
        final int levelY;
        final long timestamp;
        CachedLevel(int levelY, long time) {
            this.levelY = levelY;
            this.timestamp = time;
        }
    }

    public static Optional<Integer> getVillageLevelY(ServerLevel level, BlockPos villagePos) {
        String key = level.dimension().location() + ":" + getVillageId(villagePos);
        long now = System.currentTimeMillis();

        CachedLevel cached = villageLevelCache.get(key);
        if (cached != null && (now - cached.timestamp) < CACHE_TTL_MS) {
            return Optional.of(cached.levelY);
        }

        Optional<Integer> found = findVillageLevelY(level, villagePos);
        found.ifPresent(y -> villageLevelCache.put(key, new CachedLevel(y, now)));

        if (villageLevelCache.size() > 500) {
            villageLevelCache.entrySet().removeIf(e -> (now - e.getValue().timestamp) > CACHE_TTL_MS * 2);
        }
        return found;
    }

    private static Optional<Integer> findVillageLevelY(ServerLevel level, BlockPos villagePos) {
        try {
            PoiManager poiManager = level.getPoiManager();

            Optional<BlockPos> meeting = poiManager.findClosest(
                    holder -> holder.is(PoiTypes.MEETING), villagePos, 96, PoiManager.Occupancy.ANY);
            if (meeting.isPresent()) return Optional.of(meeting.get().getY());

            Optional<BlockPos> home = poiManager.findClosest(
                    holder -> holder.is(PoiTypes.HOME), villagePos, 96, PoiManager.Occupancy.ANY);
            if (home.isPresent()) return Optional.of(home.get().getY());

            List<net.minecraft.world.entity.npc.Villager> villagers = level.getEntitiesOfClass(
                    net.minecraft.world.entity.npc.Villager.class,
                    new net.minecraft.world.phys.AABB(villagePos).inflate(96.0D, 192.0D, 96.0D));
            if (!villagers.isEmpty()) {
                int sum = 0;
                for (net.minecraft.world.entity.npc.Villager v : villagers) {
                    sum += v.blockPosition().getY();
                }
                return Optional.of(sum / villagers.size());
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public static boolean isPlayerAtVillageLevel(ServerLevel level, net.minecraft.world.entity.player.Player player, BlockPos villagePos) {
        Optional<Integer> levelY = getVillageLevelY(level, villagePos);
        if (levelY.isEmpty()) return true;
        return Math.abs(player.blockPosition().getY() - levelY.get()) <= VILLAGE_LEVEL_RANGE;
    }

    private static String cacheKey(ServerLevel level, BlockPos pos, int radius) {
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        return level.dimension().location() + ":" + chunkX + ":" + chunkZ + ":" + radius;
    }

    public static Optional<BlockPos> findNearestVillage(ServerLevel level, BlockPos playerPos, int radius) {
        String key = cacheKey(level, playerPos, radius);
        long now = System.currentTimeMillis();

        CachedVillage cached = villageCache.get(key);
        if (cached != null && (now - cached.timestamp) < CACHE_TTL_MS) {
            return cached.villagePos;
        }

        Optional<BlockPos> vanilla = findVillageByStructure(level, playerPos, radius);
        Optional<BlockPos> claimed = findNearestCustomVillage(level, playerPos, radius);

        Optional<BlockPos> result;
        if (vanilla.isPresent() && claimed.isPresent()) {
            result = distanceSqr(playerPos, vanilla.get()) <= distanceSqr(playerPos, claimed.get())
                    ? vanilla : claimed;
        } else {
            result = vanilla.isPresent() ? vanilla : claimed;
        }

        villageCache.put(key, new CachedVillage(result, now));

        if (villageCache.size() > 500) {
            villageCache.entrySet().removeIf(e -> (now - e.getValue().timestamp) > CACHE_TTL_MS * 2);
        }

        return result;
    }

    public static boolean hasLivingVillagers(ServerLevel level, BlockPos villagePos, int checkRadius) {
        if (level == null) return true;

        int villagerCount = level.getEntitiesOfClass(
            net.minecraft.world.entity.npc.Villager.class,
            new net.minecraft.world.phys.AABB(villagePos).inflate(checkRadius, 128, checkRadius)
        ).size();
        return villagerCount > 0;
    }

    private static Optional<BlockPos> findVillageByStructure(ServerLevel level, BlockPos playerPos, int radius) {
        try {

            int radiusInChunks = Math.max(1, radius / 16);

            var structureRegistry = level.registryAccess()
                    .registryOrThrow(Registries.STRUCTURE);
            var villageTag = structureRegistry
                    .getOrCreateTag(StructureTags.VILLAGE);

            Pair<BlockPos, Holder<Structure>> result = level.getChunkSource().getGenerator()
                    .findNearestMapStructure(level, villageTag, playerPos, radiusInChunks, false);

            if (result != null) {
                BlockPos villagePos = result.getFirst();

                double dx = playerPos.getX() - villagePos.getX();
                double dz = playerPos.getZ() - villagePos.getZ();
                double distanceSqr = dx * dx + dz * dz;
                int maxRadiusSqr = (radius + 16) * (radius + 16);
                if (distanceSqr <= maxRadiusSqr) {
                    return Optional.of(villagePos);
                }
            }
        } catch (Exception e) {

        }
        return Optional.empty();
    }

    private static double distanceSqr(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    public static String getVillageId(BlockPos villagePos) {
        return VillageId.of(villagePos);
    }

    public static boolean isNearVillage(ServerLevel level, BlockPos playerPos, int radius) {
        return findNearestVillage(level, playerPos, radius).isPresent();
    }

    public static List<BlockPos> findAllVillages(ServerLevel level, BlockPos center, int radius) {

        int safeRadius = Math.min(radius, 256);
        List<BlockPos> villages = new ArrayList<>();
        PoiManager poiManager = level.getPoiManager();

        poiManager.getInRange(
                poiType -> poiType.is(PoiTypes.MEETING),
                center,
                safeRadius,
                PoiManager.Occupancy.ANY
        ).forEach(poi -> {
            BlockPos pos = poi.getPos();
            if (!villages.contains(pos)) {
                villages.add(pos);
            }
        });

        return villages;
    }

    private static Optional<BlockPos> findNearestCustomVillage(ServerLevel level, BlockPos playerPos, int radius) {
        PlayerClaimedVillageData data = PlayerClaimedVillageData.get(level);
        return data.getNearestVillage(playerPos, radius);
    }

    public static void clearCache() {
        villageCache.clear();
        villageLevelCache.clear();
    }

}
