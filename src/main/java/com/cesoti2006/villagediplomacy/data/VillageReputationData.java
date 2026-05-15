package com.cesoti2006.villagediplomacy.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class VillageReputationData extends SavedData {

    private static final String DATA_NAME = "village_diplomacy_reputation";

    // Mapa: PlayerID -> (VillagePos -> Reputation)
    private final Map<UUID, Map<String, Integer>> playerVillageReputations = new HashMap<>();

    public VillageReputationData() {
    }

    public static VillageReputationData load(CompoundTag tag) {
        VillageReputationData data = new VillageReputationData();

        ListTag playerList = tag.getList("PlayerReputations", Tag.TAG_COMPOUND);
        for (int i = 0; i < playerList.size(); i++) {
            CompoundTag playerTag = playerList.getCompound(i);
            UUID playerId = playerTag.getUUID("UUID");
            
            Map<String, Integer> villageReps = new HashMap<>();
            CompoundTag villagesTag = playerTag.getCompound("Villages");
            
            for (String villageKey : villagesTag.getAllKeys()) {
                villageReps.put(villageKey, villagesTag.getInt(villageKey));
            }
            
            data.playerVillageReputations.put(playerId, villageReps);
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag playerList = new ListTag();

        for (Map.Entry<UUID, Map<String, Integer>> playerEntry : playerVillageReputations.entrySet()) {
            CompoundTag playerTag = new CompoundTag();
            playerTag.putUUID("UUID", playerEntry.getKey());
            
            CompoundTag villagesTag = new CompoundTag();
            for (Map.Entry<String, Integer> villageEntry : playerEntry.getValue().entrySet()) {
                villagesTag.putInt(villageEntry.getKey(), villageEntry.getValue());
            }
            
            playerTag.put("Villages", villagesTag);
            playerList.add(playerTag);
        }

        tag.put("PlayerReputations", playerList);
        return tag;
    }

    public static VillageReputationData get(ServerLevel level) {
        DimensionDataStorage storage = level.getServer().overworld().getDataStorage();
        return storage.computeIfAbsent(VillageReputationData::load, VillageReputationData::new, DATA_NAME);
    }

    private String getVillageKey(BlockPos villagePos) {
        int snapX = (villagePos.getX() >> 7) << 7;
        int snapZ = (villagePos.getZ() >> 7) << 7;
        return snapX + "_" + snapZ;
    }

    public int getReputation(UUID playerId, BlockPos villagePos) {
        String villageKey = getVillageKey(villagePos);
        return playerVillageReputations
                .getOrDefault(playerId, new HashMap<>())
                .getOrDefault(villageKey, 0);
    }

    public void addReputation(UUID playerId, BlockPos villagePos, int amount) {
        String villageKey = getVillageKey(villagePos);
        Map<String, Integer> villageReps = playerVillageReputations.computeIfAbsent(playerId, k -> new HashMap<>());
        int current = villageReps.getOrDefault(villageKey, 0);
        villageReps.put(villageKey, current + amount);
        setDirty();
    }

    public void setReputation(UUID playerId, BlockPos villagePos, int reputation) {
        String villageKey = getVillageKey(villagePos);
        Map<String, Integer> villageReps = playerVillageReputations.computeIfAbsent(playerId, k -> new HashMap<>());
        villageReps.put(villageKey, reputation);
        setDirty();
    }

    // Para comandos: obtener reputación en la aldea más cercana
    public int getReputation(UUID playerId) {
        Map<String, Integer> villageReps = playerVillageReputations.get(playerId);
        if (villageReps == null || villageReps.isEmpty()) return 0;
        // Retornar el promedio de todas las aldeas como fallback
        return (int) villageReps.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public void addReputation(UUID playerId, int amount) {
        // Método legacy para compatibilidad - aplicar a TODAS las aldeas conocidas
        Map<String, Integer> villageReps = playerVillageReputations.get(playerId);
        if (villageReps != null && !villageReps.isEmpty()) {
            for (String villageKey : villageReps.keySet()) {
                int current = villageReps.get(villageKey);
                villageReps.put(villageKey, current + amount);
            }
        }
        setDirty();
    }

    public void setReputation(UUID playerId, int reputation) {
        // Método legacy para compatibilidad con comandos
        // Establecer la misma reputación en todas las aldeas conocidas
        Map<String, Integer> villageReps = playerVillageReputations.computeIfAbsent(playerId, k -> new HashMap<>());
        for (String villageKey : villageReps.keySet()) {
            villageReps.put(villageKey, reputation);
        }
        setDirty();
    }

    // Helper method: añadir/obtener reputación detectando aldea automáticamente
    public int getReputationNearby(UUID playerId, net.minecraft.server.level.ServerLevel level, BlockPos pos) {
        Optional<BlockPos> nearestVillage = com.cesoti2006.villagediplomacy.data.VillageDetector.findNearestVillage(level, pos, 200);
        if (nearestVillage.isEmpty()) return 0;
        return getReputation(playerId, nearestVillage.get());
    }

    public void addReputationNearby(UUID playerId, net.minecraft.server.level.ServerLevel level, BlockPos pos, int amount) {
        Optional<BlockPos> nearestVillage = com.cesoti2006.villagediplomacy.data.VillageDetector.findNearestVillage(level, pos, 200);
        if (nearestVillage.isEmpty()) return;
        addReputation(playerId, nearestVillage.get(), amount);
    }
    
    /**
     * Get all village reputations for a player
     * Returns map of villageId -> reputation
     */
    public Map<String, Integer> getPlayerReputations(UUID playerId) {
        return new HashMap<>(playerVillageReputations.getOrDefault(playerId, new HashMap<>()));
    }
}
