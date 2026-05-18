package com.cesoti2006.villagediplomacy.data;

import com.cesoti2006.villagediplomacy.personality.GolemPersonality;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Almacena las personalidades de todos los Iron Golems en el mundo
 */
public class GolemPersonalityData extends SavedData {
    private static final String DATA_NAME = "villagediplomacy_golems";
    
    private final Map<UUID, GolemPersonality> golemPersonalities = new HashMap<>();
    private final Map<UUID, Long> lastInteraction = new HashMap<>();
    
    public GolemPersonalityData() {
    }
    
    public static GolemPersonalityData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(GolemPersonalityData::load, GolemPersonalityData::new, DATA_NAME);
    }
    
    public static GolemPersonalityData load(CompoundTag tag) {
        GolemPersonalityData data = new GolemPersonalityData();
        
        ListTag golemsList = tag.getList("Golems", Tag.TAG_COMPOUND);
        for (int i = 0; i < golemsList.size(); i++) {
            CompoundTag golemTag = golemsList.getCompound(i);
            
            UUID golemId = UUID.fromString(golemTag.getString("GolemId"));
            String name = golemTag.getString("Name");
            GolemPersonality.GolemTrait temperament = GolemPersonality.GolemTrait.valueOf(golemTag.getString("Temperament"));
            GolemPersonality.GolemTrait loyalty = GolemPersonality.GolemTrait.valueOf(golemTag.getString("Loyalty"));

            GolemPersonality personality;
            if (golemTag.contains("StoryIdx")) {
                int idx = golemTag.getInt("StoryIdx");
                if (idx >= 0 && idx < 7) {
                    personality = new GolemPersonality(name, temperament, loyalty, "", idx);
                } else {
                    personality = new GolemPersonality(name, temperament, loyalty, golemTag.getString("Story"), -1);
                }
            } else {
                personality = new GolemPersonality(name, temperament, loyalty, golemTag.getString("Story"), -1);
            }
            data.golemPersonalities.put(golemId, personality);
        }
        
        return data;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag golemsList = new ListTag();
        
        for (Map.Entry<UUID, GolemPersonality> entry : golemPersonalities.entrySet()) {
            CompoundTag golemTag = new CompoundTag();
            GolemPersonality personality = entry.getValue();
            
            golemTag.putString("GolemId", entry.getKey().toString());
            golemTag.putString("Name", personality.getName());
            golemTag.putString("Temperament", personality.getTemperament().name());
            golemTag.putString("Loyalty", personality.getLoyalty().name());
            if (personality.getStoryTemplateIndex() >= 0) {
                golemTag.putInt("StoryIdx", personality.getStoryTemplateIndex());
                golemTag.putString("Story", "");
            } else {
                golemTag.putString("Story", personality.getLegacyStory());
            }
            
            golemsList.add(golemTag);
        }
        
        tag.put("Golems", golemsList);
        return tag;
    }
    
    /**
     * Obtener o crear personalidad para un golem
     */
    public GolemPersonality getOrCreatePersonality(UUID golemId, String villageName, Random random) {
        if (!golemPersonalities.containsKey(golemId)) {
            GolemPersonality personality = GolemPersonality.generateRandom(villageName, random);
            golemPersonalities.put(golemId, personality);
            setDirty();
        }
        return golemPersonalities.get(golemId);
    }
    
    /**
     * Obtener personalidad existente
     */
    public GolemPersonality getPersonality(UUID golemId) {
        return golemPersonalities.get(golemId);
    }
    
    /**
     * Verificar cooldown de interacción
     */
    public boolean canInteract(UUID golemId, long cooldownMs) {
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastInteraction.get(golemId);
        
        if (lastTime == null || currentTime - lastTime > cooldownMs) {
            lastInteraction.put(golemId, currentTime);
            return true;
        }
        return false;
    }
}
