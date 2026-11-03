package com.cesoti2006.villagediplomacy.data;

import com.cesoti2006.villagediplomacy.personality.*;
import com.cesoti2006.villagediplomacy.util.ModLang;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.nbt.CompoundTag;
import javax.annotation.Nonnull;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.*;

public class VillagerPersonalityData extends SavedData {
    private static final String DATA_NAME = "villagediplomacy_personality";

    private final Map<UUID, VillagerPersonality> personalities = new HashMap<>();
    private final Map<UUID, Long> deathTimes = new HashMap<>();
    private final Map<UUID, String> deadVillagerNames = new HashMap<>();

    private final Map<String, Set<String>> usedNames = new HashMap<>();

    private final Set<String> dedupedVillages = new HashSet<>();

    private static final double NAME_SEARCH_RADIUS = 80.0D;
    private static final Random NAME_RANDOM = new Random();

    public VillagerPersonalityData() {
    }

    public static VillagerPersonalityData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage()
                .computeIfAbsent(VillagerPersonalityData::load, VillagerPersonalityData::new, DATA_NAME);
    }

    public static VillagerPersonalityData load(@Nonnull CompoundTag tag) {
        VillagerPersonalityData data = new VillagerPersonalityData();

        ListTag personalitiesList = tag.getList("Personalities", Tag.TAG_COMPOUND);
        for (int i = 0; i < personalitiesList.size(); i++) {
            CompoundTag personalityTag = personalitiesList.getCompound(i);

            UUID villagerId = UUID.fromString(personalityTag.getString("VillagerId"));
            String name = personalityTag.getString("Name");
            String biome = personalityTag.getString("Biome");

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

            long birthTime = personalityTag.contains("BirthTime") ? personalityTag.getLong("BirthTime") : 0L;

            VillagerPersonality personality = new VillagerPersonality(
                villagerId, name, biome,
                courage, generosity, workEthic, socialBehavior,
                temperament, honesty, outlook,
                birthTime
            );

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

        ListTag namesList = tag.getList("UsedNames", Tag.TAG_COMPOUND);
        for (int i = 0; i < namesList.size(); i++) {
            CompoundTag nameTag = namesList.getCompound(i);
            String villageId = nameTag.getString("Village");
            String name = nameTag.getString("Name");
            if (villageId.isEmpty() || name.isEmpty()) continue;
            data.usedNames.computeIfAbsent(villageId, k -> new HashSet<>()).add(name);
        }

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

        ListTag personalitiesList = new ListTag();
        for (VillagerPersonality personality : personalities.values()) {
            CompoundTag personalityTag = new CompoundTag();

            personalityTag.putString("VillagerId", personality.getVillagerId().toString());
            personalityTag.putString("Name", personality.getCustomName());
            personalityTag.putString("Biome", personality.getBiomeType());

            personalityTag.putString("Courage", personality.getCourage().name());
            personalityTag.putString("Generosity", personality.getGenerosity().name());
            personalityTag.putString("WorkEthic", personality.getWorkEthic().name());
            personalityTag.putString("SocialBehavior", personality.getSocialBehavior().name());
            personalityTag.putString("Temperament", personality.getTemperament().name());
            personalityTag.putString("Honesty", personality.getHonesty().name());
            personalityTag.putString("Outlook", personality.getOutlook().name());
            personalityTag.putLong("BirthTime", personality.getBirthTime());

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

        ListTag usedNamesList = new ListTag();
        for (Map.Entry<String, Set<String>> entry : usedNames.entrySet()) {
            for (String name : entry.getValue()) {
                CompoundTag nameTag = new CompoundTag();
                nameTag.putString("Village", entry.getKey());
                nameTag.putString("Name", name);
                usedNamesList.add(nameTag);
            }
        }
        tag.put("UsedNames", usedNamesList);

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

    public VillagerPersonality getPersonality(UUID villagerId) {
        return personalities.get(villagerId);
    }

    public VillagerPersonality getOrCreatePersonality(Villager villager, String biomeType, Random random) {
        UUID villagerId = villager.getUUID();
        VillagerPersonality existing = personalities.get(villagerId);
        if (existing != null) {
            rememberName(VillageId.of(villager.blockPosition()), existing.getCustomName());
            return existing;
        }

        boolean isMale = random.nextBoolean();
        String villageId = VillageId.of(villager.blockPosition());
        Set<String> takenNames = namesOfVillagersAround(villager, villageId);
        String name = pickFreeName(biomeType, isMale, random, takenNames);
        rememberName(villageId, name);

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

    private Set<String> namesOfVillagersAround(Villager villager, String villageId) {
        Set<String> names = new HashSet<>(usedNames.getOrDefault(villageId, Set.of()));
        for (Villager other : villager.level().getEntitiesOfClass(
                Villager.class, villager.getBoundingBox().inflate(NAME_SEARCH_RADIUS))) {
            if (other == villager) continue;
            VillagerPersonality otherPersonality = personalities.get(other.getUUID());
            if (otherPersonality != null) {
                names.add(otherPersonality.getCustomName().toLowerCase(Locale.ROOT));
                continue;
            }

            if (other.getCustomName() != null) {
                String foreign = ModLang.stripFormatting(other.getCustomName().getString()).trim();
                if (!foreign.isEmpty()) names.add(foreign.toLowerCase(Locale.ROOT));
            }
        }
        return names;
    }

    private String pickFreeName(String biomeType, boolean isMale, Random random, Set<String> takenNames) {
        String name = firstFreeName(NameGenerator.namesFor(biomeType, isMale), takenNames, random);
        if (name == null) name = firstFreeName(NameGenerator.namesFor(biomeType, !isMale), takenNames, random);
        if (name != null) return name;

        String base = NameGenerator.generateName(biomeType, isMale, random);
        for (int n = 2; n < 100; n++) {
            String candidate = base + " " + n;
            if (!takenNames.contains(candidate.toLowerCase(Locale.ROOT))) return candidate;
        }
        return base + " " + random.nextInt(1000);
    }

    private static String firstFreeName(String[] pool, Set<String> takenNames, Random random) {
        if (pool == null || pool.length == 0) return null;
        int start = random.nextInt(pool.length);
        for (int i = 0; i < pool.length; i++) {
            String candidate = pool[(start + i) % pool.length];
            if (!takenNames.contains(candidate.toLowerCase(Locale.ROOT))) return candidate;
        }
        return null;
    }

    private void rememberName(String villageId, String name) {
        if (villageId == null || name == null || name.isEmpty()) return;
        if (usedNames.computeIfAbsent(villageId, k -> new HashSet<>())
                .add(name.toLowerCase(Locale.ROOT))) {
            setDirty();
        }
    }

    public void dedupeNames(ServerLevel level, Villager villager) {
        String villageId = VillageId.of(villager.blockPosition());
        if (!dedupedVillages.add(villageId)) return;

        List<Villager> around = level.getEntitiesOfClass(Villager.class,
                villager.getBoundingBox().inflate(NAME_SEARCH_RADIUS));

        Map<String, Villager> keepers = new HashMap<>();
        List<Villager> repeated = new ArrayList<>();

        for (Villager other : around) {
            VillagerPersonality personality = personalities.get(other.getUUID());
            if (personality == null) continue;

            String key = personality.getCustomName().toLowerCase(Locale.ROOT);
            Villager keeper = keepers.get(key);
            if (keeper == null) {
                keepers.put(key, other);
                continue;
            }
            if (other.getUUID().compareTo(keeper.getUUID()) < 0) {
                keepers.put(key, other);
                repeated.add(keeper);
            } else {
                repeated.add(other);
            }
        }

        for (Villager repeatedVillager : repeated) {
            VillagerPersonality personality = personalities.get(repeatedVillager.getUUID());
            if (personality == null) continue;

            Set<String> taken = namesOfVillagersAround(repeatedVillager, villageId);
            boolean isMale = NameGenerator.isMaleName(personality.getCustomName());
            String newName = pickFreeName(personality.getBiomeType(), isMale, NAME_RANDOM, taken);

            personality.setCustomName(newName);
            rememberName(villageId, newName);
            repeatedVillager.setCustomName(personality.getFullNameComponent());
            repeatedVillager.setCustomNameVisible(true);
        }
    }

    public void registerDeath(UUID villagerId, String villagerName) {
        deathTimes.put(villagerId, System.currentTimeMillis());
        deadVillagerNames.put(villagerId, villagerName);

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
        return elapsed < 1200000;
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
            currentTime - entry.getValue() > 1200000);
        setDirty();
    }
}
