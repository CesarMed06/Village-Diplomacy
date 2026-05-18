package com.cesoti2006.villagediplomacy.reputation;

/**
 * Gestiona ventajas/desventajas basadas en nivel de reputación
 */
public final class ReputationTiersHandler {

    public enum ReputationTier {
        // POSITIVOS
        LEGENDARY_HERO(1000, "villagediplomacy.tier.legendary_hero", 0.75f, false, false),
        HERO(800, "villagediplomacy.tier.hero", 0.80f, false, false),
        CHAMPION(500, "villagediplomacy.tier.champion", 0.85f, false, false),
        TRUSTED_FRIEND(300, "villagediplomacy.tier.trusted_friend", 0.90f, false, false),
        FRIENDLY(100, "villagediplomacy.tier.friendly", 0.95f, false, false),
        
        // NEUTRAL
        NEUTRAL(0, "villagediplomacy.tier.neutral", 1.0f, false, false),
        
        // NEGATIVOS
        SUSPICIOUS(-100, "villagediplomacy.tier.suspicious", 1.05f, false, false),
        DISLIKED(-200, "villagediplomacy.tier.disliked", 1.10f, false, false),
        UNWELCOME(-400, "villagediplomacy.tier.unwelcome", 1.15f, true, false),
        UNFRIENDLY(-700, "villagediplomacy.tier.unfriendly", 1.20f, true, false),
        HOSTILE(-900, "villagediplomacy.tier.hostile", 1.25f, true, true),
        WANTED_CRIMINAL(-1000, "villagediplomacy.tier.wanted_criminal", 1.30f, true, true);

        private final int minReputation;
        private final String translationKey;
        private final float tradeMultiplier;
        private final boolean blocksDoors;
        private final boolean blocksTrades;

        ReputationTier(int minReputation, String translationKey, float tradeMultiplier, 
                      boolean blocksDoors, boolean blocksTrades) {
            this.minReputation = minReputation;
            this.translationKey = translationKey;
            this.tradeMultiplier = tradeMultiplier;
            this.blocksDoors = blocksDoors;
            this.blocksTrades = blocksTrades;
        }

        public int getMinReputation() {
            return minReputation;
        }

        public String getTranslationKey() {
            return translationKey;
        }

        public float getTradeMultiplier() {
            return tradeMultiplier;
        }

        public boolean blocksDoors() {
            return blocksDoors;
        }

        public boolean blocksTrades() {
            return blocksTrades;
        }

        public boolean isPositive() {
            return minReputation >= 0;
        }

        public boolean isHostile() {
            return minReputation <= -900;
        }

        public int getDiscountPercent() {
            return Math.round((1.0f - tradeMultiplier) * 100);
        }

        public int getPenaltyPercent() {
            return Math.round((tradeMultiplier - 1.0f) * 100);
        }
    }

    public static ReputationTier getTierByReputation(int reputation) {
        if (reputation >= 1000) return ReputationTier.LEGENDARY_HERO;
        if (reputation >= 800)  return ReputationTier.HERO;
        if (reputation >= 500)  return ReputationTier.CHAMPION;
        if (reputation >= 300)  return ReputationTier.TRUSTED_FRIEND;
        if (reputation >= 100)  return ReputationTier.FRIENDLY;
        if (reputation >= 0)    return ReputationTier.NEUTRAL;
        if (reputation > -100)  return ReputationTier.NEUTRAL;
        if (reputation >= -199) return ReputationTier.SUSPICIOUS;
        if (reputation >= -299) return ReputationTier.DISLIKED;
        if (reputation >= -499) return ReputationTier.UNWELCOME;
        if (reputation >= -699) return ReputationTier.UNFRIENDLY;
        if (reputation >= -899) return ReputationTier.HOSTILE;
        if (reputation >= -999) return ReputationTier.HOSTILE;
        return ReputationTier.WANTED_CRIMINAL;
    }

    public static boolean canAccessDoors(int reputation) {
        ReputationTier tier = getTierByReputation(reputation);
        return !tier.blocksDoors();
    }

    public static boolean canAccessChests(int reputation) {
        ReputationTier tier = getTierByReputation(reputation);
        return reputation > -500;
    }

    public static boolean canTrade(int reputation) {
        ReputationTier tier = getTierByReputation(reputation);
        return !tier.blocksTrades();
    }

    public static float getTradeMultiplier(int reputation) {
        ReputationTier tier = getTierByReputation(reputation);
        return tier.getTradeMultiplier();
    }

    public static String getTierDescription(int reputation) {
        ReputationTier tier = getTierByReputation(reputation);
        return "villagediplomacy.tier." + tier.name().toLowerCase();
    }
}
