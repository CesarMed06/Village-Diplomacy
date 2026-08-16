package com.cesoti2006.villagediplomacy.data;

import com.cesoti2006.villagediplomacy.util.VillageNameGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.*;

public class PlayerClaimedVillageData extends SavedData {

    private static final String DATA_NAME = "village_diplomacy_player_claimed";

    private final Map<String, ClaimedVillage> villages = new HashMap<>();

    public PlayerClaimedVillageData() {
    }

    public static class ClaimedVillage {
        public final String id;
        public final BlockPos center;
        public final String name;
        public final UUID owner;
        public final int radius;

        public ClaimedVillage(String id, BlockPos center, String name, UUID owner, int radius) {
            this.id = id;
            this.center = center;
            this.name = name;
            this.owner = owner;
            this.radius = radius;
        }
    }

    public static PlayerClaimedVillageData load(CompoundTag tag) {
        PlayerClaimedVillageData data = new PlayerClaimedVillageData();

        ListTag villagesList = tag.getList("Villages", Tag.TAG_COMPOUND);
        for (int i = 0; i < villagesList.size(); i++) {
            CompoundTag vTag = villagesList.getCompound(i);
            String id = vTag.getString("Id");
            int x = vTag.getInt("X");
            int y = vTag.getInt("Y");
            int z = vTag.getInt("Z");
            String name = vTag.getString("Name");
            UUID owner = UUID.fromString(vTag.getString("Owner"));
            int radius = vTag.getInt("Radius");
            data.villages.put(id, new ClaimedVillage(id, new BlockPos(x, y, z), name, owner, radius));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag villagesList = new ListTag();
        for (ClaimedVillage v : villages.values()) {
            CompoundTag vTag = new CompoundTag();
            vTag.putString("Id", v.id);
            vTag.putInt("X", v.center.getX());
            vTag.putInt("Y", v.center.getY());
            vTag.putInt("Z", v.center.getZ());
            vTag.putString("Name", v.name);
            vTag.putString("Owner", v.owner.toString());
            vTag.putInt("Radius", v.radius);
            villagesList.add(vTag);
        }
        tag.put("Villages", villagesList);
        return tag;
    }

    public static PlayerClaimedVillageData get(ServerLevel level) {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(PlayerClaimedVillageData::load, PlayerClaimedVillageData::new, DATA_NAME);
    }

    public Optional<BlockPos> getNearestVillage(BlockPos playerPos, int searchRadius) {
        BlockPos nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        for (ClaimedVillage v : villages.values()) {
            double distSq = playerPos.distSqr(v.center);

            if (distSq > (double)(searchRadius * searchRadius)) continue;

            if (distSq <= (double)(v.radius * v.radius) && distSq < nearestDistSq) {
                nearest = v.center;
                nearestDistSq = distSq;
            }
        }
        return nearest != null ? Optional.of(nearest) : Optional.empty();
    }

    public boolean addVillage(BlockPos center, String name, UUID owner, int radius) {
        String id = "player_" + center.getX() + "_" + center.getZ();
        if (villages.containsKey(id)) return false;
        villages.put(id, new ClaimedVillage(id, center, name, owner, radius));
        setDirty();
        return true;
    }

    public boolean removeVillage(BlockPos center, UUID owner) {
        String id = "player_" + center.getX() + "_" + center.getZ();
        ClaimedVillage v = villages.get(id);
        if (v == null) return false;
        if (!v.owner.equals(owner)) return false;
        villages.remove(id);
        setDirty();
        return true;
    }

    public ClaimedVillage getVillageAt(BlockPos pos) {
        for (ClaimedVillage v : villages.values()) {
            if (v.center.getX() == pos.getX() && v.center.getZ() == pos.getZ()) {
                return v;
            }
        }
        return null;
    }

    public boolean isTooCloseToAny(BlockPos center, int minDistance) {
        for (ClaimedVillage v : villages.values()) {
            if (center.distSqr(v.center) < (double)(minDistance * minDistance)) {
                return true;
            }
        }
        return false;
    }

    public Collection<ClaimedVillage> getAllVillages() {
        return villages.values();
    }
}
