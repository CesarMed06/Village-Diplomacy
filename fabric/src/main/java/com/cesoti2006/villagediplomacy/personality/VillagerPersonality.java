package com.cesoti2006.villagediplomacy.personality;

import java.util.UUID;

/**
 * Complete personality system for a villager (EXPANDED)
 */
public class VillagerPersonality {
    private final UUID villagerId;
    private final String customName;
    private final String biomeType;
    
    // EXPANDED PERSONALITY TRAITS (7 dimensions)
    private final PersonalityTrait courage;        // COWARD -> FEARLESS
    private final PersonalityTrait generosity;     // GREEDY -> CHARITABLE
    private final PersonalityTrait workEthic;      // LAZY -> WORKAHOLIC
    private final PersonalityTrait socialBehavior; // SHY -> EXTROVERTED
    private final PersonalityTrait temperament;    // CALM -> HOTHEADED
    private final PersonalityTrait honesty;        // CUNNING -> TRUSTWORTHY
    private final PersonalityTrait outlook;        // PESSIMISTIC -> CHEERFUL
    private final long birthTime;
    
    // Emotional state
    private EmotionalState currentEmotion = EmotionalState.NEUTRAL;
    private long emotionChangeTime = 0;
    
    // Relationships
    private int playerReputationBonus = 0;
    private UUID savedByPlayer = null;
    private long lastSavedTime = 0;
    
    // Profession
    private String profession = "";
    private int professionalLevel = 1;
    private String title = "";
    
    public VillagerPersonality(UUID villagerId, String customName, String biomeType, 
                              PersonalityTrait courage, PersonalityTrait generosity,
                              PersonalityTrait workEthic, PersonalityTrait socialBehavior,
                              PersonalityTrait temperament, PersonalityTrait honesty,
                              PersonalityTrait outlook) {
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
        this.birthTime = System.currentTimeMillis();
    }
    
    // Getters
    public UUID getVillagerId() { return villagerId; }
    public String getCustomName() { return customName; }
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
        // Si la emoción actual ha expirado, volver a NEUTRAL
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
        // Límites: -100 a +100
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
    
    public String getTitle() { return title; }
    
    private void updateTitle() {
        if (professionalLevel >= 4) {
            switch (profession.toLowerCase()) {
                case "librarian": title = "the Wise"; break;
                case "weaponsmith": title = "the Smith"; break;
                case "armorer": title = "the Anvil"; break;
                case "toolsmith": title = "the Craftsman"; break;
                case "farmer": title = "the Farmer"; break;
                case "fisherman": title = "the Fisher"; break;
                case "shepherd": title = "the Shepherd"; break;
                case "fletcher": title = "the Archer"; break;
                case "butcher": title = "the Butcher"; break;
                case "leatherworker": title = "the Tanner"; break;
                case "mason": title = "the Mason"; break;
                case "cartographer": title = "the Cartographer"; break;
                case "cleric": title = "the Healer"; break;
                default: title = "the Expert"; break;
            }
        } else {
            title = "";
        }
    }
    
    public String getFullName() {
        if (title.isEmpty()) {
            return customName;
        }
        return customName + " " + title;
    }
    
    /**
     * Calculate price multiplier based on personality and relationship
     */
    public float getPriceMultiplier(UUID playerId, int baseReputation) {
        float multiplier = 1.0f;
        
        // Personal bonus if you saved their life
        if (playerId.equals(savedByPlayer)) {
            long timeSinceSave = System.currentTimeMillis() - lastSavedTime;
            if (timeSinceSave < 3600000) { // 1 hour
                multiplier -= 0.3f; // 30% discount
            } else if (timeSinceSave < 7200000) { // 2 hours
                multiplier -= 0.15f; // 15% discount
            }
        }
        
        // Modifier by generosity (EXPANDED)
        switch (generosity) {
            case CHARITABLE:
                if (baseReputation > 0) multiplier -= 0.20f; // 20% extra discount
                else if (baseReputation >= 0) multiplier -= 0.10f; // 10% even if neutral
                break;
            case GENEROUS:
                if (baseReputation > 0) multiplier -= 0.15f; // 15% extra discount
                break;
            case THRIFTY:
                if (baseReputation < 0) multiplier += 0.10f; // 10% more expensive if bad rep
                break;
            case GREEDY:
                multiplier += 0.20f; // 20% more expensive even if hero
                break;
            case NEUTRAL_GENEROSITY:
            default:
                break;
        }
        
        // Modifier by honesty (EXPANDED)
        switch (honesty) {
            case CUNNING:
                multiplier += 0.15f; // 15% markup, tries to trick you
                break;
            case SHREWD:
                multiplier += 0.08f; // 8% markup, business-minded
                break;
            case HONEST:
                multiplier -= 0.05f; // 5% discount, fair pricing
                break;
            case TRUSTWORTHY:
                multiplier -= 0.10f; // 10% discount, very fair
                break;
            case NEUTRAL_HONESTY:
            default:
                break;
        }
        
        // Personal reputation bonus
        if (playerReputationBonus > 50) {
            multiplier -= 0.2f; // 20% discount
        } else if (playerReputationBonus > 25) {
            multiplier -= 0.1f; // 10% discount
        } else if (playerReputationBonus < -25) {
            multiplier += 0.2f; // 20% more expensive
        }
        
        return Math.max(0.3f, Math.min(2.0f, multiplier));
    }
}
