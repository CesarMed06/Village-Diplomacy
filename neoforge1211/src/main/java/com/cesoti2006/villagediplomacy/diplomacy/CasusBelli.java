package com.cesoti2006.villagediplomacy.diplomacy;

import java.util.Locale;

public enum CasusBelli {

    NONE(0, WarGoal.NONE),
    BORDER_DISPUTE(20, WarGoal.BORDER),
    OLD_GRUDGE(15, WarGoal.SUBMISSION),
    INCIDENT(25, WarGoal.TRIBUTE),
    SABOTAGE(25, WarGoal.TRIBUTE),
    BETRAYAL(30, WarGoal.VENGEANCE);

    private static final String KEY_PREFIX = "villagediplomacy.casusbelli.";

    private final int weight;

    private final WarGoal warGoal;

    CasusBelli(int weight, WarGoal warGoal) {
        this.weight = weight;
        this.warGoal = warGoal;
    }

    public int weight() {
        return weight;
    }

    public boolean isPresent() {
        return this != NONE;
    }

    public WarGoal warGoal() {
        return warGoal;
    }

    public String getTranslationKey() {
        return KEY_PREFIX + name().toLowerCase(Locale.ROOT);
    }

    public static CasusBelli strongest(CasusBelli first, CasusBelli second) {
        if (first == null) return second == null ? NONE : second;
        if (second == null) return first;
        return first.weight >= second.weight ? first : second;
    }

    public static CasusBelli fromString(String value) {
        if (value == null) return NONE;
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return NONE;
        }
    }
}
