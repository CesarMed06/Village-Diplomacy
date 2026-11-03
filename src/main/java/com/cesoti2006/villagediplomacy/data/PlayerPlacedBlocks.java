package com.cesoti2006.villagediplomacy.data;

import com.cesoti2006.villagediplomacy.config.VillageDiplomacyConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerPlacedBlocks extends SavedData {

    private static final String DATA_NAME = "village_diplomacy_placed_blocks";

    private static final long DAY_MS = 24L * 60L * 60L * 1000L;
    private static final long DEFAULT_RETENTION_MS = 90L * DAY_MS;
    private static final long PURGE_INTERVAL_MS = 10L * 60L * 1000L;
    private static final int MAX_ENTRIES = 100_000;

    private record PlacedBlock(UUID playerId, long placedAt) {}

    private final Map<String, PlacedBlock> placedBlocks = new HashMap<>();
    private long lastPurge;

    public PlayerPlacedBlocks() {
    }

    public static PlayerPlacedBlocks load(CompoundTag tag) {
        PlayerPlacedBlocks data = new PlayerPlacedBlocks();
        long now = System.currentTimeMillis();
        ListTag blockList = tag.getList("PlacedBlocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockList.size(); i++) {
            CompoundTag entry = blockList.getCompound(i);
            String key = entry.getString("Key");
            UUID placer = entry.getUUID("Placer");

            long placedAt = entry.contains("PlacedAt") ? entry.getLong("PlacedAt") : now;
            data.placedBlocks.put(key, new PlacedBlock(placer, placedAt));
        }
        data.purge(now);
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag blockList = new ListTag();
        for (Map.Entry<String, PlacedBlock> entry : placedBlocks.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Key", entry.getKey());
            entryTag.putUUID("Placer", entry.getValue().playerId());
            entryTag.putLong("PlacedAt", entry.getValue().placedAt());
            blockList.add(entryTag);
        }
        tag.put("PlacedBlocks", blockList);
        return tag;
    }

    public static PlayerPlacedBlocks get(ServerLevel level) {
        if (level.getServer() == null) return new PlayerPlacedBlocks();
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(PlayerPlacedBlocks::load, PlayerPlacedBlocks::new, DATA_NAME);
    }

    private static String makeKey(ServerLevel level, BlockPos pos) {
        return level.dimension().location() + ":" + pos.getX() + ":" + pos.getY() + ":" + pos.getZ();
    }

    private static long retentionMillis() {
        if (!VillageDiplomacyConfig.SPEC.isLoaded()) return DEFAULT_RETENTION_MS;
        return VillageDiplomacyConfig.INSTANCE.placedBlockRetentionDays.get() * DAY_MS;
    }

    public void recordPlace(ServerLevel level, BlockPos pos, UUID playerId) {
        long now = System.currentTimeMillis();
        placedBlocks.put(makeKey(level, pos), new PlacedBlock(playerId, now));
        setDirty();

        if (now - lastPurge >= PURGE_INTERVAL_MS) {
            purge(now);
        }
    }

    public boolean isPlacedBy(ServerLevel level, BlockPos pos, UUID playerId) {
        PlacedBlock placed = placedBlocks.get(makeKey(level, pos));
        return placed != null && placed.playerId().equals(playerId);
    }

    public boolean hasOwner(ServerLevel level, BlockPos pos) {
        return placedBlocks.containsKey(makeKey(level, pos));
    }

    public boolean isPlayerBuiltZone(ServerLevel level, BlockPos pos, UUID playerId) {
        if (isPlacedBy(level, pos, playerId)) return true;
        if (isPlacedBy(level, pos.below(), playerId)) return true;
        int owned = 0;
        int floorY = pos.getY() - 1;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                BlockPos floorPos = new BlockPos(pos.getX() + dx, floorY, pos.getZ() + dz);
                if (!level.isLoaded(floorPos)) continue;
                if (isPlacedBy(level, floorPos, playerId)) {
                    owned++;
                    if (owned >= 2) return true;
                }
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 4; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos near = pos.offset(dx, dy, dz);
                    if (!level.isLoaded(near)) continue;
                    if (isPlacedBy(level, near, playerId)) return true;
                }
            }
        }
        return false;
    }

    private void purge(long now) {
        lastPurge = now;
        long cutoff = now - retentionMillis();
        int before = placedBlocks.size();

        placedBlocks.entrySet().removeIf(entry -> entry.getValue().placedAt() < cutoff);

        if (placedBlocks.size() > MAX_ENTRIES) {
            List<Map.Entry<String, PlacedBlock>> oldestFirst = new ArrayList<>(placedBlocks.entrySet());
            oldestFirst.sort(Comparator.comparingLong(e -> e.getValue().placedAt()));

            int toDrop = oldestFirst.size() - MAX_ENTRIES;
            for (int i = 0; i < toDrop; i++) {
                placedBlocks.remove(oldestFirst.get(i).getKey());
            }
        }

        if (placedBlocks.size() != before) {
            setDirty();
        }
    }
}
