package com.cesoti2006.villagediplomacy.data;

import com.cesoti2006.villagediplomacy.personality.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

/**
 * Almacena las personalidades de todos los aldeanos en el mundo
 */
public class VillagerPersonalityData extends SavedData {
    private static final String DATA_NAME = "villagediplomacy_personality";
    
    private final Map<UUID, VillagerPersonality> personalities = new HashMap<>();
    private final Map<UUID, Long> deathTimes = new HashMap<>(); // UUID aldeano -> tiempo de muerte
    private final Map<UUID, String> deadVillagerNames = new HashMap<>(); // Para el luto
    
    public VillagerPersonalityData() {
    }
    
    public static VillagerPersonalityData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(VillagerPersonalityData::load, VillagerPersonalityData::new, DATA_NAME);
    }
    
    public static VillagerPersonalityData load(CompoundTag tag) {
        VillagerPersonalityData data = new VillagerPersonalityData();
        
        // Load personalities
        ListTag personalitiesList = tag.getList("Personalities", Tag.TAG_COMPOUND);
        for (int i = 0; i < personalitiesList.size(); i++) {
            CompoundTag personalityTag = personalitiesList.getCompound(i);
            
            UUID villagerId = UUID.fromString(personalityTag.getString("VillagerId"));
            String name = personalityTag.getString("Name");
            String biome = personalityTag.getString("Biome");
            
            // Load all 7 personality traits
            PersonalityTrait courage = PersonalityTrait.valueOf(personalityTag.getString("Courage"));
            PersonalityTrait generosity = PersonalityTrait.valueOf(personalityTag.getString("Generosity"));
            PersonalityTrait workEthic = personalityTag.contains("WorkEthic") ? 
                PersonalityTrait.valueOf(personalityTag.getString("WorkEthic")) : PersonalityTrait.NEUTRAL_WORK;
            PersonalityTrait socialBehavior = personalityTag.contains("SocialBehavior") ? 
                PersonalityTrait.valueOf(personalityTag.getString("SocialBehavior")) : PersonalityTrait.NEUTRAL_SOCIAL;
            PersonalityTrait temperament = personalityTag.contains("Temperament") ? 
                PersonalityTrait.valueOf(personalityTag.getString("Temperament")) : PersonalityTrait.NEUTRAL;
            PersonalityTrait honesty = personalityTag.contains("Honesty") ? 
                PersonalityTrait.valueOf(personalityTag.getString("Honesty")) : PersonalityTrait.NEUTRAL_HONESTY;
            PersonalityTrait outlook = personalityTag.contains("Outlook") ? 
                PersonalityTrait.valueOf(personalityTag.getString("Outlook")) : PersonalityTrait.NEUTRAL_OUTLOOK;
            
            VillagerPersonality personality = new VillagerPersonality(
                villagerId, name, biome, 
                courage, generosity, workEthic, socialBehavior,
                temperament, honesty, outlook
            );
            
            // Load additional data
            if (personalityTag.contains("Emotion")) {
                personality.setCurrentEmotion(EmotionalState.valueOf(personalityTag.getString("Emotion")));
            }
            if (personalityTag.contains("ReputationBonus")) {
                int bonus = personalityTag.getInt("ReputationBonus");
                personality.addPlayerReputationBonus(bonus);
            }
            if (personalityTag.contains("SavedBy")) {
                UUID savedBy = UUID.fromString(personalityTag.getString("SavedBy"));

                personality.setSavedByPlayer(savedBy);
            }
            if (personalityTag.contains("Profession")) {
                personality.setProfession(personalityTag.getString("Profession"));
            }
            if (personalityTag.contains("Level")) {
                personality.setProfessionalLevel(personalityTag.getInt("Level"));
            }
            
            data.personalities.put(villagerId, personality);
        }
        
        // Cargar muertes
        ListTag deathsList = tag.getList("Deaths", Tag.TAG_COMPOUND);
        for (int i = 0; i < deathsList.size(); i++) {
            CompoundTag deathTag = deathsList.getCompound(i);
            UUID villagerId = UUID.fromString(deathTag.getString("VillagerId"));
            long deathTime = deathTag.getLong("DeathTime");
            String name = deathTag.getString("Name");
            
            data.deathTimes.put(villagerId, deathTime);
            data.deadVillagerNames.put(villagerId, name);
        }
        
        return data;
    }
    
    @Override
    public CompoundTag save(CompoundTag tag) {
        // Save personalities
        ListTag personalitiesList = new ListTag();
        for (VillagerPersonality personality : personalities.values()) {
            CompoundTag personalityTag = new CompoundTag();
            
            personalityTag.putString("VillagerId", personality.getVillagerId().toString());
            personalityTag.putString("Name", personality.getCustomName());
            personalityTag.putString("Biome", personality.getBiomeType());
            
            // Save all 7 personality traits
            personalityTag.putString("Courage", personality.getCourage().name());
            personalityTag.putString("Generosity", personality.getGenerosity().name());
            personalityTag.putString("WorkEthic", personality.getWorkEthic().name());
            personalityTag.putString("SocialBehavior", personality.getSocialBehavior().name());
            personalityTag.putString("Temperament", personality.getTemperament().name());
            personalityTag.putString("Honesty", personality.getHonesty().name());
            personalityTag.putString("Outlook", personality.getOutlook().name());
            
            personalityTag.putString("Emotion", personality.getCurrentEmotion().name());
            personalityTag.putInt("ReputationBonus", personality.getPlayerReputationBonus());
            
            if (personality.getSavedByPlayer() != null) {
                personalityTag.putString("SavedBy", personality.getSavedByPlayer().toString());
            }
            if (!personality.getProfession().isEmpty()) {
                personalityTag.putString("Profession", personality.getProfession());
            }
            personalityTag.putInt("Level", personality.getProfessionalLevel());
            
            personalitiesList.add(personalityTag);
        }
        tag.put("Personalities", personalitiesList);
        
        // Guardar muertes
        ListTag deathsList = new ListTag();
        for (Map.Entry<UUID, Long> entry : deathTimes.entrySet()) {
            CompoundTag deathTag = new CompoundTag();
            deathTag.putString("VillagerId", entry.getKey().toString());
            deathTag.putLong("DeathTime", entry.getValue());
            deathTag.putString("Name", deadVillagerNames.get(entry.getKey()));
            deathsList.add(deathTag);
        }
        tag.put("Deaths", deathsList);
        
        return tag;
    }
    
    // ========== MÉTODOS PÚBLICOS ==========
    
    public VillagerPersonality getPersonality(UUID villagerId) {
        return personalities.get(villagerId);
    }
    
    public VillagerPersonality getOrCreatePersonality(UUID villagerId, String biomeType, Random random) {
        if (personalities.containsKey(villagerId)) {
            return personalities.get(villagerId);
        }
        
        // Create new personality with ALL 7 traits
        boolean isMale = random.nextBoolean();
        String name = NameGenerator.generateName(biomeType, isMale, random);
        PersonalityTrait courage = PersonalityTrait.randomCourage(random);
        PersonalityTrait generosity = PersonalityTrait.randomGenerosity(random);
        PersonalityTrait workEthic = PersonalityTrait.randomWorkEthic(random);
        PersonalityTrait socialBehavior = PersonalityTrait.randomSocialBehavior(random);
        PersonalityTrait temperament = PersonalityTrait.randomTemperament(random);
        PersonalityTrait honesty = PersonalityTrait.randomHonesty(random);
        PersonalityTrait outlook = PersonalityTrait.randomOutlook(random);
        
        VillagerPersonality personality = new VillagerPersonality(
            villagerId, name, biomeType, 
            courage, generosity, workEthic, socialBehavior,
            temperament, honesty, outlook
        );
        personalities.put(villagerId, personality);
        setDirty();
        
        return personality;
    }
    
    public void registerDeath(UUID villagerId, String villagerName) {
        deathTimes.put(villagerId, System.currentTimeMillis());
        deadVillagerNames.put(villagerId, villagerName);
        
        // Todos los aldeanos del pueblo entran en luto
        for (VillagerPersonality personality : personalities.values()) {
            if (!personality.getVillagerId().equals(villagerId)) {
                personality.setCurrentEmotion(EmotionalState.MOURNING);
            }
        }
        
        setDirty();
    }
    
    public boolean isRecentDeath(UUID villagerId) {
        if (!deathTimes.containsKey(villagerId)) return false;
        long deathTime = deathTimes.get(villagerId);
        long elapsed = System.currentTimeMillis() - deathTime;
        return elapsed < 1200000; // 20 minutos
    }
    
    public String getDeadVillagerName(UUID villagerId) {
        return deadVillagerNames.get(villagerId);
    }
    
    public List<VillagerPersonality> getAllPersonalities() {
        return new ArrayList<>(personalities.values());
    }
    
    public void cleanupOldDeaths() {
        long currentTime = System.currentTimeMillis();
        deathTimes.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > 1200000); // 20 minutos
        setDirty();
    }
}
