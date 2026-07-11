package com.cesoti2006.villagediplomacy.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import javax.annotation.Nonnull;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class PlayerPlacedBlocks extends SavedData {

    private static final String DATA_NAME = "village_diplomacy_placed_blocks";

    
    private final Map<String, UUID> placedBlocks = new HashMap<>();

    public PlayerPlacedBlocks() {
    }

    public static PlayerPlacedBlocks load(CompoundTag tag) {
        PlayerPlacedBlocks data = new PlayerPlacedBlocks();
        ListTag blockList = tag.getList("PlacedBlocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blockList.size(); i++) {
            CompoundTag entry = blockList.getCompound(i);
            String key = entry.getString("Key");
            UUID placer = entry.getUUID("Placer");
            data.placedBlocks.put(key, placer);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag blockList = new ListTag();
        for (Map.Entry<String, UUID> entry : placedBlocks.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putString("Key", entry.getKey());
            entryTag.putUUID("Placer", entry.getValue());
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

    public void recordPlace(ServerLevel level, BlockPos pos, UUID playerId) {
        String key = makeKey(level, pos);
        placedBlocks.put(key, playerId);
        setDirty();
    }

    public void removePlace(ServerLevel level, BlockPos pos) {
        String key = makeKey(level, pos);
        placedBlocks.remove(key);
        setDirty();
    }

    public boolean isPlacedBy(ServerLevel level, BlockPos pos, UUID playerId) {
        UUID owner = placedBlocks.get(makeKey(level, pos));
        return owner != null && owner.equals(playerId);
    }

    public UUID getPlacer(ServerLevel level, BlockPos pos) {
        return placedBlocks.get(makeKey(level, pos));
    }

    public boolean hasOwner(ServerLevel level, BlockPos pos) {
        return placedBlocks.containsKey(makeKey(level, pos));
    }
}
