package com.cesoti2006.villagediplomacy.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.UUID;

public final class BlockEntityPersistent {

    private static final String ROOT = "VillageDiplomacyData";

    private BlockEntityPersistent() {}

    public static CompoundTag read(BlockEntity blockEntity) {
        if (blockEntity == null) return new CompoundTag();
        CompoundTag all = blockEntity.saveWithoutMetadata();
        return all.contains(ROOT, Tag.TAG_COMPOUND) ? all.getCompound(ROOT) : new CompoundTag();
    }

    public static void write(BlockEntity blockEntity, CompoundTag data) {
        if (blockEntity == null) return;
        CompoundTag all = blockEntity.saveWithoutMetadata();
        all.put(ROOT, data);
        blockEntity.load(all);
        blockEntity.setChanged();
    }

    public static boolean hasUUID(BlockEntity blockEntity, String key) {
        return read(blockEntity).hasUUID(key);
    }

    public static UUID getUUID(BlockEntity blockEntity, String key) {
        CompoundTag data = read(blockEntity);
        return data.hasUUID(key) ? data.getUUID(key) : null;
    }

    public static void putUUID(BlockEntity blockEntity, String key, UUID value) {
        CompoundTag data = read(blockEntity);
        data.putUUID(key, value);
        write(blockEntity, data);
    }
}
