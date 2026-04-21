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

public class VillageRelationshipData extends SavedData {

    private static final String DATA_NAME = "village_diplomacy_relationships";

    private final Map<String, Map<String, Integer>> relationships = new HashMap<>();
    private final Map<String, BlockPos> villagePositions = new HashMap<>();
    private final Map<String, String> villageNames = new HashMap<>();

    public VillageRelationshipData() {
    }

    public static VillageRelationshipData load(CompoundTag tag) {
        VillageRelationshipData data = new VillageRelationshipData();

        ListTag relationshipsList = tag.getList("Relationships", Tag.TAG_COMPOUND);
        for (int i = 0; i < relationshipsList.size(); i++) {
            CompoundTag relationTag = relationshipsList.getCompound(i);
            String village1 = relationTag.getString("Village1");
            String village2 = relationTag.getString("Village2");
            int points = relationTag.getInt("Points");

            data.relationships.computeIfAbsent(village1, k -> new HashMap<>()).put(village2, points);
            data.relationships.computeIfAbsent(village2, k -> new HashMap<>()).put(village1, points);
        }

        ListTag positionsList = tag.getList("VillagePositions", Tag.TAG_COMPOUND);
        for (int i = 0; i < positionsList.size(); i++) {
            CompoundTag posTag = positionsList.getCompound(i);
            String villageId = posTag.getString("Id");
            int x = posTag.getInt("X");
            int y = posTag.getInt("Y");
            int z = posTag.getInt("Z");
            data.villagePositions.put(villageId, new BlockPos(x, y, z));
        }

        ListTag namesList = tag.getList("VillageNames", Tag.TAG_COMPOUND);
        for (int i = 0; i < namesList.size(); i++) {
            CompoundTag nameTag = namesList.getCompound(i);
            String villageId = nameTag.getString("Id");
            String name = nameTag.getString("Name");
            data.villageNames.put(villageId, name);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag relationshipsList = new ListTag();
        Set<String> processed = new HashSet<>();

        for (Map.Entry<String, Map<String, Integer>> entry : relationships.entrySet()) {
            String village1 = entry.getKey();
            for (Map.Entry<String, Integer> relation : entry.getValue().entrySet()) {
                String village2 = relation.getKey();
                String key = village1.compareTo(village2) < 0 ?
                        village1 + ":" + village2 : village2 + ":" + village1;

                if (!processed.contains(key)) {
                    CompoundTag relationTag = new CompoundTag();
                    relationTag.putString("Village1", village1);
                    relationTag.putString("Village2", village2);
                    relationTag.putInt("Points", relation.getValue());
                    relationshipsList.add(relationTag);
                    processed.add(key);
                }
            }
        }
        tag.put("Relationships", relationshipsList);

        ListTag positionsList = new ListTag();
        for (Map.Entry<String, BlockPos> entry : villagePositions.entrySet()) {
            CompoundTag posTag = new CompoundTag();
            posTag.putString("Id", entry.getKey());
            BlockPos pos = entry.getValue();
            posTag.putInt("X", pos.getX());
            posTag.putInt("Y", pos.getY());
            posTag.putInt("Z", pos.getZ());
            positionsList.add(posTag);
        }
        tag.put("VillagePositions", positionsList);

        ListTag namesList = new ListTag();
        for (Map.Entry<String, String> entry : villageNames.entrySet()) {
            CompoundTag nameTag = new CompoundTag();
            nameTag.putString("Id", entry.getKey());
            nameTag.putString("Name", entry.getValue());
            namesList.add(nameTag);
        }
        tag.put("VillageNames", namesList);

        return tag;
    }

    public static VillageRelationshipData get(ServerLevel level) {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(VillageRelationshipData::load, VillageRelationshipData::new, DATA_NAME);
    }

    public void registerVillage(BlockPos pos) {
        String villageId = getVillageId(pos);
        if (!villagePositions.containsKey(villageId)) {
            villagePositions.put(villageId, pos);

            if (!villageNames.containsKey(villageId)) {
                String generatedName = VillageNameGenerator.generateNameFromPosition(pos.getX(), pos.getZ());
                villageNames.put(villageId, generatedName);
            }

            setDirty();
        }
    }

    public String getVillageId(BlockPos pos) {
        return "village_" + (pos.getX() >> 4) + "_" + (pos.getZ() >> 4);
    }

    public String getVillageName(String villageId) {
        return villageNames.getOrDefault(villageId, villageId);
    }

    public void setVillageName(String villageId, String name) {
        villageNames.put(villageId, name);
        setDirty();
    }

    public int getRelationship(String village1, String village2) {
        if (village1.equals(village2)) return 100;
        return relationships.getOrDefault(village1, new HashMap<>()).getOrDefault(village2, 0);
    }

    public void setRelationship(String village1, String village2, int points) {
        points = Math.max(-100, Math.min(100, points));

        relationships.computeIfAbsent(village1, k -> new HashMap<>()).put(village2, points);
        relationships.computeIfAbsent(village2, k -> new HashMap<>()).put(village1, points);

        setDirty();
    }

    public void addRelationship(String village1, String village2, int delta) {
        int current = getRelationship(village1, village2);
        setRelationship(village1, village2, current + delta);
    }

    public RelationshipStatus getStatus(String village1, String village2) {
        int points = getRelationship(village1, village2);
        if (points >= 50) return RelationshipStatus.ALLIED;
        if (points <= -50) return RelationshipStatus.HOSTILE;
        return RelationshipStatus.NEUTRAL;
    }

    public Map<String, BlockPos> getAllVillages() {
        return new HashMap<>(villagePositions);
    }

    public BlockPos getVillagePosition(String villageId) {
        return villagePositions.get(villageId);
    }

    public enum RelationshipStatus {
        ALLIED("villagediplomacy.rel.allied"),
        NEUTRAL("villagediplomacy.rel.neutral_village"),
        HOSTILE("villagediplomacy.rel.hostile_village");

        private final String translationKey;

        RelationshipStatus(String translationKey) {
            this.translationKey = translationKey;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        /** @deprecated Prefer {@link #getTranslationKey()} with {@code Component.translatable(...)} on the client. */
        @Deprecated
        public String getDisplay() {
            return translationKey;
        }
    }
}
