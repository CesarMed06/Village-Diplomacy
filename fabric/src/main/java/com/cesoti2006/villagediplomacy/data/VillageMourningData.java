package com.cesoti2006.villagediplomacy.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Rastrea muertes de aldeanos para el sistema de luto
 * Aldeanos cercanos se acercan al puesto de trabajo vacío y lloran
 */
public class VillageMourningData extends SavedData {
    private static final String DATA_NAME = "villagediplomacy_mourning";
    
    // Estructura: villageId -> lista de muertes recientes
    private final Map<String, List<MourningRecord>> recentDeaths = new HashMap<>();
    
    public VillageMourningData() {
    }
    
    public static VillageMourningData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(VillageMourningData::load, VillageMourningData::new, DATA_NAME);
    }
    
    public static VillageMourningData load(CompoundTag tag) {
        VillageMourningData data = new VillageMourningData();
        
        ListTag deathsList = tag.getList("Deaths", Tag.TAG_COMPOUND);
        for (int i = 0; i < deathsList.size(); i++) {
            CompoundTag deathTag = deathsList.getCompound(i);
            
            String villageId = deathTag.getString("VillageId");
            String villagerName = deathTag.getString("VillagerName");
            String profession = deathTag.getString("Profession");
            long deathTime = deathTag.getLong("DeathTime");
            
            BlockPos jobSite = null;
            if (deathTag.contains("JobSite")) {
                CompoundTag posTag = deathTag.getCompound("JobSite");
                jobSite = new BlockPos(
                    posTag.getInt("X"),
                    posTag.getInt("Y"),
                    posTag.getInt("Z")
                );
            }
            
            MourningRecord record = new MourningRecord(villagerName, profession, deathTime, jobSite);
            data.recentDeaths.computeIfAbsent(villageId, k -> new ArrayList<>()).add(record);
        }
        
        return data;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag deathsList = new ListTag();
        
        for (Map.Entry<String, List<MourningRecord>> entry : recentDeaths.entrySet()) {
            String villageId = entry.getKey();
            for (MourningRecord record : entry.getValue()) {
                CompoundTag deathTag = new CompoundTag();
                deathTag.putString("VillageId", villageId);
                deathTag.putString("VillagerName", record.villagerName);
                deathTag.putString("Profession", record.profession);
                deathTag.putLong("DeathTime", record.deathTime);
                
                if (record.jobSitePos != null) {
                    CompoundTag posTag = new CompoundTag();
                    posTag.putInt("X", record.jobSitePos.getX());
                    posTag.putInt("Y", record.jobSitePos.getY());
                    posTag.putInt("Z", record.jobSitePos.getZ());
                    deathTag.put("JobSite", posTag);
                }
                
                deathsList.add(deathTag);
            }
        }
        
        tag.put("Deaths", deathsList);
        return tag;
    }
    
    /**
     * Registrar muerte de aldeano
     */
    public void registerDeath(String villageId, String villagerName, String profession, BlockPos jobSite) {
        long currentTime = System.currentTimeMillis();
        MourningRecord record = new MourningRecord(villagerName, profession, currentTime, jobSite);
        
        recentDeaths.computeIfAbsent(villageId, k -> new ArrayList<>()).add(record);
        setDirty();
    }
    
    /**
     * Obtener muertes recientes (últimas 24 horas in-game = 20 min real)
     */
    public List<MourningRecord> getRecentDeaths(String villageId) {
        long currentTime = System.currentTimeMillis();
        long mourningPeriod = 20 * 60 * 1000; // 20 minutos reales
        
        List<MourningRecord> deaths = recentDeaths.get(villageId);
        if (deaths == null) return Collections.emptyList();
        
        // Filtrar muertes antiguas
        deaths.removeIf(death -> currentTime - death.deathTime > mourningPeriod);
        
        if (deaths.isEmpty()) {
            recentDeaths.remove(villageId);
            setDirty();
        }
        
        return new ArrayList<>(deaths);
    }
    
    /**
     * Limpiar muertes antiguas
     */
    public void cleanup() {
        long currentTime = System.currentTimeMillis();
        long mourningPeriod = 20 * 60 * 1000;
        
        recentDeaths.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(death -> currentTime - death.deathTime > mourningPeriod);
            return entry.getValue().isEmpty();
        });
        
        setDirty();
    }
    
    /**
     * Registro de muerte
     */
    public static class MourningRecord {
        public final String villagerName;
        public final String profession;
        public final long deathTime;
        public final BlockPos jobSitePos;
        
        public MourningRecord(String villagerName, String profession, long deathTime, BlockPos jobSitePos) {
            this.villagerName = villagerName;
            this.profession = profession;
            this.deathTime = deathTime;
            this.jobSitePos = jobSitePos;
        }
        
        public long getTimeSinceDeath() {
            return System.currentTimeMillis() - deathTime;
        }
        
        public boolean hasJobSite() {
            return jobSitePos != null;
        }
    }
}
