package com.cesoti2006.villagediplomacy.personality;

import java.util.UUID;

public class VillagerPersonality {
    private final UUID villagerId;
    private String customName;
    private final String biomeType;

    private final PersonalityTrait courage;
    private final PersonalityTrait generosity;
    private final PersonalityTrait workEthic;
    private final PersonalityTrait socialBehavior;
    private final PersonalityTrait temperament;
    private final PersonalityTrait honesty;
    private final PersonalityTrait outlook;
    private final long birthTime;

    private EmotionalState currentEmotion = EmotionalState.NEUTRAL;
    private long emotionChangeTime = 0;

    private int playerReputationBonus = 0;
    private UUID savedByPlayer = null;
    private long lastSavedTime = 0;

    private String profession = "";
    private int professionalLevel = 1;
    private String titleKey = "";

    public VillagerPersonality(UUID villagerId, String customName, String biomeType,
                              PersonalityTrait courage, PersonalityTrait generosity,
                              PersonalityTrait workEthic, PersonalityTrait socialBehavior,
                              PersonalityTrait temperament, PersonalityTrait honesty,
                              PersonalityTrait outlook) {
        this(villagerId, customName, biomeType, courage, generosity, workEthic, socialBehavior,
                temperament, honesty, outlook, System.currentTimeMillis());
    }

    public VillagerPersonality(UUID villagerId, String customName, String biomeType,
                              PersonalityTrait courage, PersonalityTrait generosity,
                              PersonalityTrait workEthic, PersonalityTrait socialBehavior,
                              PersonalityTrait temperament, PersonalityTrait honesty,
                              PersonalityTrait outlook, long birthTime) {
        this.villagerId = villagerId;
        this.customName = customName;
        this.biomeType = biomeType;
        this.courage = courage;
        this.generosity = generosity;
        this.workEthic = workEthic;
        this.socialBehavior = socialBehavior;
        this.temperament = temperament;
        this.honesty = honesty;
        this.outlook = outlook;
        this.birthTime = birthTime > 0 ? birthTime : System.currentTimeMillis();
    }

    public UUID getVillagerId() { return villagerId; }
    public String getCustomName() { return customName; }

    public void setCustomName(String customName) { this.customName = customName; }
    public String getBiomeType() { return biomeType; }
    public PersonalityTrait getCourage() { return courage; }
    public PersonalityTrait getGenerosity() { return generosity; }
    public PersonalityTrait getWorkEthic() { return workEthic; }
    public PersonalityTrait getSocialBehavior() { return socialBehavior; }
    public PersonalityTrait getTemperament() { return temperament; }
    public PersonalityTrait getHonesty() { return honesty; }
    public PersonalityTrait getOutlook() { return outlook; }
    public long getBirthTime() { return birthTime; }

    public EmotionalState getCurrentEmotion() { return currentEmotion; }
    public void setCurrentEmotion(EmotionalState emotion) {
        this.currentEmotion = emotion;
        this.emotionChangeTime = System.currentTimeMillis();
    }

    public void updateEmotion() {

        if (currentEmotion != EmotionalState.NEUTRAL) {
            long elapsed = System.currentTimeMillis() - emotionChangeTime;
            if (elapsed >= currentEmotion.getDuration()) {
                setCurrentEmotion(EmotionalState.NEUTRAL);
            }
        }
    }

    public long getEmotionChangeTime() { return emotionChangeTime; }

    public int getPlayerReputationBonus() { return playerReputationBonus; }
    public void addPlayerReputationBonus(int amount) {
        this.playerReputationBonus += amount;

        if (this.playerReputationBonus > 100) this.playerReputationBonus = 100;
        if (this.playerReputationBonus < -100) this.playerReputationBonus = -100;
    }

    public UUID getSavedByPlayer() { return savedByPlayer; }
    public void setSavedByPlayer(UUID playerId) {
        this.savedByPlayer = playerId;
        this.lastSavedTime = System.currentTimeMillis();
    }
    public long getLastSavedTime() { return lastSavedTime; }

    public String getProfession() { return profession; }
    public void setProfession(String profession) { this.profession = profession; }

    public int getProfessionalLevel() { return professionalLevel; }
    public void setProfessionalLevel(int level) {
        this.professionalLevel = Math.min(5, Math.max(1, level));
        updateTitle();
    }

    private void updateTitle() {
        if (professionalLevel < 4) {
            titleKey = "";
            return;
        }
        String prof = profession.toLowerCase();
        int colon = prof.indexOf(':');
        if (colon >= 0) {
            prof = prof.substring(colon + 1);
        }
        switch (prof) {
            case "librarian": titleKey = "villagediplomacy.title.librarian"; break;
            case "weaponsmith": titleKey = "villagediplomacy.title.weaponsmith"; break;
            case "armorer": titleKey = "villagediplomacy.title.armorer"; break;
            case "toolsmith": titleKey = "villagediplomacy.title.toolsmith"; break;
            case "farmer": titleKey = "villagediplomacy.title.farmer"; break;
            case "fisherman": titleKey = "villagediplomacy.title.fisherman"; break;
            case "shepherd": titleKey = "villagediplomacy.title.shepherd"; break;
            case "fletcher": titleKey = "villagediplomacy.title.fletcher"; break;
            case "butcher": titleKey = "villagediplomacy.title.butcher"; break;
            case "leatherworker": titleKey = "villagediplomacy.title.leatherworker"; break;
            case "mason": titleKey = "villagediplomacy.title.mason"; break;
            case "cartographer": titleKey = "villagediplomacy.title.cartographer"; break;
            case "cleric": titleKey = "villagediplomacy.title.cleric"; break;
            default: titleKey = "villagediplomacy.title.default"; break;
        }
    }

    public net.minecraft.network.chat.Component getFullNameComponent() {
        net.minecraft.network.chat.MutableComponent namePart = net.minecraft.network.chat.Component
                .literal(customName)
                .withStyle(net.minecraft.ChatFormatting.GOLD);
        if (titleKey.isEmpty()) {
            return namePart;
        }
        return namePart.append(net.minecraft.network.chat.Component.literal(" "))
                .append(net.minecraft.network.chat.Component.translatable(titleKey)
                        .withStyle(net.minecraft.ChatFormatting.GRAY)
                        .withStyle(net.minecraft.ChatFormatting.ITALIC));
    }

    public float getPriceMultiplier(UUID playerId, int baseReputation) {
        float multiplier = 1.0f;

        if (playerId.equals(savedByPlayer)) {
            long timeSinceSave = System.currentTimeMillis() - lastSavedTime;
            if (timeSinceSave < 3600000) {
                multiplier -= 0.3f;
            } else if (timeSinceSave < 7200000) {
                multiplier -= 0.15f;
            }
        }

        switch (generosity) {
            case CHARITABLE:
                if (baseReputation > 0) multiplier -= 0.20f;
                else if (baseReputation >= 0) multiplier -= 0.10f;
                break;
            case GENEROUS:
                if (baseReputation > 0) multiplier -= 0.15f;
                break;
            case THRIFTY:
                if (baseReputation < 0) multiplier += 0.10f;
                break;
            case GREEDY:
                multiplier += 0.20f;
                break;
            case NEUTRAL_GENEROSITY:
            default:
                break;
        }

        switch (honesty) {
            case CUNNING:
                multiplier += 0.15f;
                break;
            case SHREWD:
                multiplier += 0.08f;
                break;
            case HONEST:
                multiplier -= 0.05f;
                break;
            case TRUSTWORTHY:
                multiplier -= 0.10f;
                break;
            case NEUTRAL_HONESTY:
            default:
                break;
        }

        if (playerReputationBonus > 50) {
            multiplier -= 0.2f;
        } else if (playerReputationBonus > 25) {
            multiplier -= 0.1f;
        } else if (playerReputationBonus < -25) {
            multiplier += 0.2f;
        }

        return Math.max(0.3f, Math.min(2.0f, multiplier));
    }
}
